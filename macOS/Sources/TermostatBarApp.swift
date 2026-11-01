import SwiftUI

@main
struct TermostatBarApp: App {
    @StateObject private var model = ThermostatModel()

    var body: some Scene {
        MenuBarExtra {
            PanelView(model: model)
        } label: {
            BarLabel(model: model)
        }
        .menuBarExtraStyle(.window)
    }
}

struct BarLabel: View {
    @ObservedObject var model: ThermostatModel

    var body: some View {
        HStack(spacing: 2) {
            Image(systemName: model.error != nil
                ? "exclamationmark.triangle.fill"
                : (!model.isOn ? "thermometer.medium.slash" : (model.isHeating ? "flame.fill" : "thermometer.medium")))
            if let current = model.current {
                Text(String(format: "%.1f°", current))
            }
        }
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
                if model.isOn {
                    Label(model.isHeating ? "Escalfant" : "En repòs", systemImage: model.isHeating ? "flame.fill" : "pause.circle")
                        .font(.caption)
                        .foregroundStyle(model.isHeating ? .orange : .secondary)
                }
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

            HStack(spacing: 8) {
                ForEach([20.0, 21.0, 23.0], id: \.self) { value in
                    Button("\(Int(value))°") { model.setTarget(value) }
                        .buttonStyle(.bordered)
                        .tint(model.target == value ? .accentColor : nil)
                }
            }
            .disabled(!model.isOn)

            if model.mode != nil {
                ScheduleView(model: model)
                    .disabled(!model.isOn)
            }

            Toggle("Pausa (encès, sense escalfar)", isOn: Binding(get: { model.isPaused }, set: model.setPaused))
                .font(.caption)
                .toggleStyle(.checkbox)
                .frame(maxWidth: .infinity, alignment: .leading)
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

struct ScheduleView: View {
    @ObservedObject var model: ThermostatModel

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Picker("Mode", selection: Binding(
                get: { model.isAuto ? "auto" : "home" },
                set: { model.setMode($0) }
            )) {
                Text("Automàtic").tag("auto")
                Text("Manual").tag("home")
            }
            .pickerStyle(.segmented)
            .labelsHidden()

            if model.isTemporary {
                HStack {
                    Label(
                        "Temporal" + (model.nextSlot().map { " fins a les \($0.time)" } ?? "")
                            + String(format: " · %.1f°C", model.target),
                        systemImage: "hand.tap"
                    )
                    .foregroundStyle(.orange)
                    Spacer()
                    Button("Tornar al programa") { model.setMode("auto") }
                        .buttonStyle(.link)
                }
                .font(.caption)
            }

            if model.isAuto, !model.schedule.isEmpty {
                // Es recalcula cada minut perquè el tram actiu canviï encara que no arribin dades noves
                TimelineView(.everyMinute) { context in
                    let current = model.currentSlot(at: context.date)
                    VStack(spacing: 2) {
                        ForEach(Array(model.schedule.enumerated()), id: \.element.id) { index, slot in
                            let isCurrent = slot == current
                            let isPast = current.map { slot.minute < $0.minute } ?? false
                            let end = index + 1 < model.schedule.count ? model.schedule[index + 1].time : model.schedule[0].time
                            HStack {
                                Image(systemName: isCurrent ? "arrowtriangle.right.fill" : "circle.fill")
                                    .font(.system(size: isCurrent ? 8 : 4))
                                    .frame(width: 10)
                                Text("\(slot.time) – \(end)").monospacedDigit()
                                Spacer()
                                Text(String(format: "%.1f°C", slot.target)).monospacedDigit()
                            }
                            .font(.caption.weight(isCurrent ? .semibold : .regular))
                            .foregroundStyle(isCurrent ? AnyShapeStyle(.tint) : (isPast ? AnyShapeStyle(.tertiary) : AnyShapeStyle(.secondary)))
                            .padding(.vertical, 2)
                            .padding(.horizontal, 6)
                            .background(isCurrent ? AnyShapeStyle(.tint.opacity(0.12)) : AnyShapeStyle(.clear), in: .rect(cornerRadius: 5))
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
