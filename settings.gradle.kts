rootProject.name = "CosmosLookingGlass"

includeBuild("kool") {
    dependencySubstitution {
        substitute(module("kool:kool-core")).using(project(":kool-core"))
    }
}