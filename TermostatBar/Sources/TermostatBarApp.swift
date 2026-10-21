import SwiftUI

@main
struct TermostatBarApp: App {
    @StateObject private var model = ThermostatModel()

    var body: some Scene {
        MenuBarExtra {
            PanelView(model: model)
        } label: {
            Image(systemName: model.error != nil
                ? "exclamationmark.triangle.fill"
                : (model.isOn ? "thermometer.medium" : "thermometer.medium.slash"))
            if let current = model.current {
                Text(String(format: "%.1f°", current))
            }
        }
        .menuBarExtraStyle(.window)
    }
}

struct PanelView: View {
    @ObservedObject var model: ThermostatModel

    var body: some View {
        VStack(spacing: 14) {
            HStack {
                Text("Termòstat").font(.headline)
                Spacer()
                Toggle("", isOn: Binding(get: { model.isOn }, set: model.setPower))
                    .toggleStyle(.switch)
                    .labelsHidden()
            }

            VStack(spacing: 2) {
                Text(model.current.map { String(format: "%.1f°C", $0) } ?? "--")
                    .font(.system(size: 40, weight: .semibold, design: .rounded))
                Text("temperatura actual").font(.caption).foregroundStyle(.secondary)
            }

            HStack(spacing: 18) {
                Button { model.adjustTarget(by: -0.5) } label: { Image(systemName: "minus.circle.fill") }
                VStack(spacing: 0) {
                    Text(String(format: "%.1f°C", model.target)).font(.title2.monospacedDigit())
                    Text("consigna").font(.caption).foregroundStyle(.secondary)
                }
                .frame(minWidth: 80)
                Button { model.adjustTarget(by: 0.5) } label: { Image(systemName: "plus.circle.fill") }
            }
            .buttonStyle(.plain)
            .font(.title)
            .disabled(!model.isOn)

            if let error = model.error {
                VStack(alignment: .leading, spacing: 6) {
                    Text(error).font(.caption.bold()).foregroundStyle(.red)
                    if let help = model.help {
                        Text(help).font(.caption).foregroundStyle(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                        Button("Obrir iot.tuya.com") { NSWorkspace.shared.open(TuyaError.consoleURL) }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            Divider()

            Toggle("Iniciar en arrencar", isOn: Binding(get: { model.launchAtLogin }, set: model.setLaunchAtLogin))
                .font(.caption)
                .toggleStyle(.checkbox)
                .frame(maxWidth: .infinity, alignment: .leading)

            HStack {
                if let battery = model.battery {
                    Label("\(battery)%", systemImage: "battery.100").font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                Button { Task { await model.refresh() } } label: { Image(systemName: "arrow.clockwise") }
                    .disabled(model.loading)
                Button("Sortir") { NSApplication.shared.terminate(nil) }
            }
            .buttonStyle(.borderless)
        }
        .padding(16)
        .frame(width: 280)
        .task { await model.refresh() }
    }
}
