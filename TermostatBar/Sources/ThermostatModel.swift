import Foundation

@MainActor
final class ThermostatModel: ObservableObject {
    @Published var isOn = false
    @Published var current: Double?
    @Published var target = 20.0
    @Published var minTarget = 5.0
    @Published var maxTarget = 35.0
    @Published var battery: Int?
    @Published var error: String?
    @Published var loading = false

    private var client: TuyaClient?
    private var pendingTarget: Task<Void, Never>?

    init() {
        do {
            client = TuyaClient(config: try Config.load())
        } catch {
            self.error = "Falta o és invàlid \(Config.url.path)"
            return
        }
        Task { [weak self] in
            while !Task.isCancelled {
                await self?.refresh()
                try? await Task.sleep(for: .seconds(120))
            }
        }
    }

    func refresh() async {
        guard let client else { return }
        loading = true
        defer { loading = false }
        do {
            let status = try await client.status()
            isOn = status.isOn
            current = status.current
            minTarget = status.minTarget
            maxTarget = status.maxTarget
            battery = status.battery
            if pendingTarget == nil, let remote = status.target { target = remote }
            error = nil
        } catch {
            self.error = error.localizedDescription
        }
    }

    func setPower(_ on: Bool) {
        let previous = isOn
        isOn = on
        Task {
            do {
                try await client?.send(code: "switch", value: on)
                error = nil
            } catch {
                isOn = previous
                self.error = error.localizedDescription
            }
        }
    }

    func adjustTarget(by delta: Double) {
        target = min(maxTarget, max(minTarget, target + delta))
        pendingTarget?.cancel()
        let value = Int((target * 10).rounded())
        pendingTarget = Task {
            try? await Task.sleep(for: .milliseconds(800))
            guard !Task.isCancelled else { return }
            do {
                try await client?.send(code: "temp_set", value: value)
                error = nil
            } catch {
                self.error = error.localizedDescription
            }
            pendingTarget = nil
        }
    }
}
