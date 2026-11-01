// swift-tools-version:6.0
import PackageDescription

let package = Package(
    name: "TermostatBar",
    platforms: [.macOS(.v14)],
    targets: [
        .executableTarget(name: "TermostatBar", path: "Sources")
    ],
    swiftLanguageModes: [.v5]
)
