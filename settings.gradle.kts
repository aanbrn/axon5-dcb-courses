// SPDX-License-Identifier: MIT
pluginManagement { includeBuild("build-logic") }

plugins { id("courses.settings") }

rootProject.name = "axon5-dcb-courses"

include(
    ":platform",
    ":tooling:convention-sample",
)

// Anticipated role-based layout (D-03); each phase adds the modules it needs:
//   services/{command,query,automation,bff}, contracts/*, frontend, docs-site
