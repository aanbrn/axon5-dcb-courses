// SPDX-License-Identifier: MIT
pluginManagement { includeBuild("build-logic") }

plugins { id("courses.settings") }

rootProject.name = "axon5-dcb-courses"

// Role-based layout (D-01, D-03). Backend code lives under `backend/`:
//   backend/contracts/{kernel,commands,events,queries}                  flat projects `:contracts-*`
//   backend/{command-service,query-service,automation-service,bff}      flat projects named like their compose
//                                                                       services and images
// `platform/`, `build-logic/`, `frontend/`, `docs-site/` and `tooling/` stay at the top level. Each phase adds the
// modules it needs; flat names are mapped to their directories through `projectDir`.

include(
    ":platform",
    ":contracts-kernel",
)

project(":contracts-kernel").projectDir = file("backend/contracts/kernel")
