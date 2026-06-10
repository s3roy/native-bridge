// swift-tools-version:5.7
import PackageDescription

let package = Package(
    name: "NativeWebViewBridge",
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: "NativeWebViewBridge",
            targets: ["NativeWebViewBridge"]
        )
    ],
    targets: [
        .target(
            name: "NativeWebViewBridge",
            path: "Sources/NativeWebViewBridge"
        )
    ]
)
