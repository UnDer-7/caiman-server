rootProject.name = "caiman-server"

include("caiman-contracts")
project(":caiman-contracts").projectDir = file("caiman-shared/contracts")
include("caiman-web-support")
project(":caiman-web-support").projectDir = file("caiman-shared/web-support")

include("caiman-debtor-core")
project(":caiman-debtor-core").projectDir = file("caiman-debtor/core")
include("caiman-debtor-entrypoint")
project(":caiman-debtor-entrypoint").projectDir = file("caiman-debtor/entrypoint")
include("caiman-debtor-infrastructure")
project(":caiman-debtor-infrastructure").projectDir = file("caiman-debtor/infrastructure")

include("caiman-billing-core")
project(":caiman-billing-core").projectDir = file("caiman-billing/core")
include("caiman-billing-entrypoint")
project(":caiman-billing-entrypoint").projectDir = file("caiman-billing/entrypoint")
include("caiman-billing-infrastructure")
project(":caiman-billing-infrastructure").projectDir = file("caiman-billing/infrastructure")

include("caiman-payment-core")
project(":caiman-payment-core").projectDir = file("caiman-payment/core")
include("caiman-payment-entrypoint")
project(":caiman-payment-entrypoint").projectDir = file("caiman-payment/entrypoint")
include("caiman-payment-infrastructure")
project(":caiman-payment-infrastructure").projectDir = file("caiman-payment/infrastructure")

include("caiman-notification-core")
project(":caiman-notification-core").projectDir = file("caiman-notification/core")
include("caiman-notification-entrypoint")
project(":caiman-notification-entrypoint").projectDir = file("caiman-notification/entrypoint")
include("caiman-notification-infrastructure")
project(":caiman-notification-infrastructure").projectDir = file("caiman-notification/infrastructure")

include("caiman-app")
