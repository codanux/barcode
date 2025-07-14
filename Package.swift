// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Barcode",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "Barcode",
            targets: ["BarcodePlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "BarcodePlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/BarcodePlugin"),
        .testTarget(
            name: "BarcodePluginTests",
            dependencies: ["BarcodePlugin"],
            path: "ios/Tests/BarcodePluginTests")
    ]
)