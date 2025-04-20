import contextlib
import itertools
import math
import operator
import os
import string
import time
from pathlib import Path

import click
import cv2
import moderngl
import numpy as np
import tqdm
from spatium import Vec2i

gl = moderngl.create_context(require=460, standalone=True)
print("GL_RENDERER:", gl.info["GL_RENDERER"])


@contextlib.contextmanager
def timed_step(name: str):
    t = time.perf_counter()
    print(name, end="")
    yield
    print(f" {time.perf_counter() - t:.2f}s")


def make_shader(multisample_level: int, mode: str):
    # language=glsl
    shader_src = \
        """
        #version 460

        layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;

        #define MULTISAMPLE $multisample_level
        #ifndef MULTISAMPLE
        #define MULTISAMPLE 0
        #endif
        #define MULTISAMPLE_GRID (1 + (MULTISAMPLE * 2))

        #define PI 3.141592653589793
        #define PI_D 3.141592653589793LF
        #define TAU 6.283185307179586
        #define TAU_D 6.283185307179586LF

        uniform uvec3 uInvocationIDOrigin;

        uniform uvec2 uEquirecMapSize;
        layout(std430, binding=0) readonly buffer uEquirecMapBlcok {
            uint uEquirecMap[];
        };

        layout(binding=0, rgba8) uniform writeonly imageCube uCubeMap;

        // https://outerra.blogspot.com/2014/05/double-precision-approximations-for-map.html
        // atan2 approximation for doubles for GLSL
        // using http://lolengine.net/wiki/doc/maths/remez
        double atan2(double y, double x) {
            //@formatter:off
            const double atan_tbl[] = {
            -3.333333333333333333333333333303396520128e-1LF,
            1.999999117496509842004185053319506031014e-1LF,
            -1.428514132711481940637283859690014415584e-1LF,
            1.110012236849539584126568416131750076191e-1LF,
            -8.993611617787817334566922323958104463948e-2LF,
            7.212338962134411520637759523226823838487e-2LF,
            -5.205055255952184339031830383744136009889e-2LF,
            2.938542391751121307313459297120064977888e-2LF,
            -1.079891788348568421355096111489189625479e-2LF,
            1.858552116405489677124095112269935093498e-3LF
            };
        
            /* argument reduction: 
               arctan (-x) = -arctan(x); 
               arctan (1/x) = 1/2 * pi - arctan (x), when x > 0
            */
        
            double ax = abs(x);
            double ay = abs(y);
            double t0 = max(ax, ay);
            double t1 = min(ax, ay);
        
            double a = 1 / t0;
            a *= t1;
        
            double s = a * a;
            double p = atan_tbl[9];
        
            p = fma( fma( fma( fma( fma( fma( fma( fma( fma( fma(p, s,
            atan_tbl[8]), s,
            atan_tbl[7]), s,
            atan_tbl[6]), s,
            atan_tbl[5]), s,
            atan_tbl[4]), s,
            atan_tbl[3]), s,
            atan_tbl[2]), s,
            atan_tbl[1]), s,
            atan_tbl[0]), s*a, a);
        
            double r = ay > ax ? (1.57079632679489661923LF - p) : p;
        
            r = x < 0 ?  3.14159265358979323846LF - r : r;
            r = y < 0 ? -r : r;
        
            return r;
            //@formatter:on
        }

        ivec2 imod(ivec2 a, ivec2 b) {
            return (a % b + b) % b;
        }

        vec4 _fetchEquirec(ivec2 texCoord) {
            uvec2 uTexCoord = uvec2(imod(texCoord, ivec2(uEquirecMapSize)));
            uint value = uEquirecMap[uTexCoord.y * uEquirecMapSize.x + uTexCoord.x];
            return unpackUnorm4x8(value);
        }

        vec4 _sampleEquirec(dvec2 uv) {
            uv *= uEquirecMapSize;
            ivec2 paa = ivec2(floor(uv));
            ivec2 pba = paa + ivec2(1, 0);
            ivec2 pab = paa + ivec2(0, 1);
            ivec2 pbb = paa + ivec2(1, 1);
            vec2 blend = vec2(fract(uv));
            vec4 aa = _fetchEquirec(paa);
            vec4 ba = _fetchEquirec(pba);
            vec4 ab = _fetchEquirec(pab);
            vec4 bb = _fetchEquirec(pbb);
            vec4 ma = mix(aa, ba, blend.x);
            vec4 mb = mix(ab, bb, blend.x);
            return mix(ma, mb, blend.y);
        }

        vec4 sampleEquirec(dvec3 dir) {
            return _sampleEquirec(dvec2(
            (atan2(dir.z, dir.x) + PI_D) / TAU_D,
            (atan2(dir.y, length(dir.xz)) + (PI_D / 2)) / PI_D
            ));
        }

        dvec3 cubeUV2Dir(dvec2 uv, int layer) {
            uv = uv * 2 - 1;
            dvec3 dir = dvec3(0);
            //@formatter:off
            switch (layer) {
                case 0:
                    dir = dvec3(-1.0, uv.x, uv.y);
                    break;
                case 1:
                    dir = dvec3(1.0, uv.x, uv.y);
                    break;
                case 2:
                    dir = dvec3(uv.x, -1.0, uv.y);
                    break;
                case 3:
                    dir = dvec3(uv.x, 1.0, uv.y);
                    break;
                case 4:
                    dir = dvec3(uv.x, uv.y, -1.0);
                    break;
                case 5:
                    dir = dvec3(uv.x, uv.y, 1.0);
                    break;
            }
            //@formatter:on
            return normalize(dir);
        }

        struct Sample {
            dvec3 dir;
            vec4 value;
        };

        Sample sampleEquirecForCubeMap(dvec2 texCoord, int layer) {
            dvec3 dir = cubeUV2Dir(texCoord / dvec2(imageSize(uCubeMap)), layer);
            return Sample(dir, sampleEquirec(dir));
        }

        vec4 mixSamples(in Sample samples[MULTISAMPLE_GRID][MULTISAMPLE_GRID]) {
            #if $mode_is_color
            dvec4 acc = dvec4(0);
            for (int x = 0; x < MULTISAMPLE_GRID; x++) {
                for (int y = 0; y < MULTISAMPLE_GRID; y++) {
                    acc += samples[y][x].value;
                }
            }
            return vec4(acc / (MULTISAMPLE_GRID * MULTISAMPLE_GRID));
            #elif $mode_is_normal_map
            #endif
        }

        void main() {
            int cubeMapSize = imageSize(uCubeMap).x;
            ivec2 iTexCoord = ivec2(uInvocationIDOrigin.xy + gl_GlobalInvocationID.xy);
            if (iTexCoord.x >= cubeMapSize || iTexCoord.y >= cubeMapSize) return;
            Sample samples[MULTISAMPLE_GRID][MULTISAMPLE_GRID];
            dvec2 texCoord = dvec2(iTexCoord) + 0.5LF;
            int layer = int(uInvocationIDOrigin.z + gl_GlobalInvocationID.z);
            for (int my = -MULTISAMPLE; my <= MULTISAMPLE; my++) {
                for (int mx = -MULTISAMPLE; mx <= MULTISAMPLE; mx++) {
                    dvec2 offset = dvec2(mx, my) / (double(MULTISAMPLE) + 0.5LF) / 2;
                    samples[my + MULTISAMPLE][mx + MULTISAMPLE] = sampleEquirecForCubeMap(texCoord + offset, layer);
                }
            }
            imageStore(uCubeMap, ivec3(iTexCoord, layer), mixSamples(samples));
        }
        """
    if mode == "normal":
        raise NotImplementedError("normal map mode")
    shader_src = string.Template(shader_src).substitute(
        multisample_level=multisample_level,
        mode_is_color=int(mode == "color"),
        mode_is_normal_map=int(mode == "normal_map")
    )
    return gl.compute_shader(shader_src)


def run_compute(shader: moderngl.ComputeShader, size: int):
    batch_size = 8
    batches = math.ceil(size / 8 / batch_size)
    for layer, batch in tqdm.tqdm(itertools.product(range(6), range(batches)), total=6 * batches):
        with gl.query(time=True) as query:
            shader["uInvocationIDOrigin"] = (0, batch * 8 * batch_size, layer)
            shader.run(math.ceil(size / 8), batch_size, 1)
        _ = query.elapsed


@operator.call
@click.command("cubemapper")
@click.option("--input", "input_path", required=True, type=click.Path(exists=True, file_okay=True, dir_okay=False),
              help="Input equirectangular image path.")
@click.option("--output", "output_path", required=True, type=click.Path(file_okay=True, dir_okay=False),
              help="Output cubemap image path, suffixes like '_neg_x' or '_pos_y' are appended to the the file name.")
@click.option("--size", "cubemap_size", required=True, type=click.IntRange(1, 16384, clamp=True),
              help="Size of output cubemap.")
@click.option("--mode", "mode", type=click.Choice(["color", "normal_map"]), default="color")
@click.option("--multisample", type=click.IntRange(-1, 100, clamp=True), default=-1,
              help="Multisample level to use, the multisample grid with be a square grid with width of (<this value> * 2 + 1). Use -1 to use automatically calculated level based on in/out texture size.")
def main(
    input_path: os.PathLike[str],
    output_path: os.PathLike[str],
    cubemap_size: int,
    mode: str,
    multisample: int
):
    with timed_step("Loading equirec image..."):
        equirec_img = cv2.imread(input_path, cv2.IMREAD_UNCHANGED)
        if len(equirec_img.shape) == 3:
            equirec_img = cv2.cvtColor(equirec_img, cv2.COLOR_RGB2RGBA)
        equirec_buffer = gl.buffer(equirec_img)

    if multisample == -1:
        equirec_face_size = max(equirec_img.shape[1] / 4, equirec_img.shape[0] / 2)
        scale_factor = equirec_face_size / cubemap_size
        multisample = math.ceil((scale_factor / 2) * 2.0)

    print(f"Multisample level: {multisample}")

    with timed_step("Compiling shader..."):
        shader = make_shader(multisample, mode)

    with timed_step("Setup compute shader..."):
        cubemap = gl.texture_cube([cubemap_size] * 2, components=4)

        cubemap.bind_to_image(unit=0, read=False, write=True)
        shader["uCubeMap"] = 0
        equirec_buffer.bind_to_storage_buffer(binding=0)
        shader["uEquirecMapSize"] = Vec2i(*equirec_img.shape[0:2]).yx

    print("Running compute shader...")
    run_compute(shader, cubemap.size[0])

    with timed_step("Saving cubemap"):
        suffixes = ["neg_x", "pos_x", "neg_y", "pos_y", "neg_z", "pos_z"]
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        face_buffer = np.empty([*cubemap.size, 4], np.uint8)
        for i in range(6):
            cubemap.read_into(face_buffer, i)
            cv2.imwrite(output_path.with_stem(f"{output_path.stem}_{suffixes[i]}"), face_buffer)
            print(".", end="")
