import Foundation
import ServiceManagement

@MainActor
final class ThermostatModel: ObservableObject {
    @Published var help: String?
    @Published var launchAtLogin = SMAppService.mainApp.status == .enabled
    @Published var isOn = false
    @Published var isHeating = false
    @Published var resumeTarget: Double? = UserDefaults.standard.object(forKey: "resumeTarget") as? Double
    private var pauseTarget: Double? = UserDefaults.standard.object(forKey: "pauseTarget") as? Double
    @Published var current: Double?
    @Published var target = 20.0
    @Published var minTarget = 5.0
    @Published var maxTarget = 35.0
    @Published var battery: Int?
    @Published var error: String?
    @Published var loading = false

    private var client: TuyaClient?
    private var pendingTarget: Task<Void, Never>?

    private let activity = ProcessInfo.processInfo.beginActivity(
        options: .userInitiatedAllowingIdleSystemSleep, reason: "Refresc periòdic del termòstat")

    init() {
        if !UserDefaults.standard.bool(forKey: "didRegisterLaunchAtLogin") {
            UserDefaults.standard.set(true, forKey: "didRegisterLaunchAtLogin")
            setLaunchAtLogin(true)
        }
        do {
            client = TuyaClient(config: try Config.load())
        } catch {
            self.error = "Falta o és invàlid \(Config.url.path)"
            return
        }
        Task { [weak self] in
            while !Task.isCancelled {
                await self?.refresh()
                try? await Task.sleep(for: .seconds(60))
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
            isHeating = status.isHeating
            current = status.current
            minTarget = status.minTarget
            maxTarget = status.maxTarget
            battery = status.battery
            if pendingTarget == nil, let remote = status.target {
                target = remote
                if resumeTarget != nil, remote != pauseTarget { storePause(resume: nil, pause: nil) }
            }
            clearError()
        } catch {
            report(error)
        }
    }

    func setLaunchAtLogin(_ on: Bool) {
        do {
            if on { try SMAppService.mainApp.register() } else { try SMAppService.mainApp.unregister() }
        } catch {
            report(error)
        }
        launchAtLogin = SMAppService.mainApp.status == .enabled
    }

    private func clearError() {
        error = nil
        help = nil
    }

    private func report(_ failure: Error) {
        if failure is URLError {
            error = "Sense connexió a internet"
            help = nil
        } else {
            error = failure.localizedDescription
            help = (failure as? TuyaError)?.instructions
        }
    }

    func setPower(_ on: Bool) {
        let previous = isOn
        isOn = on
        if !on { isHeating = false }
        Task {
            do {
                try await client?.send(code: "switch", value: on)
                clearError()
            } catch {
                isOn = previous
                report(error)
            }
        }
    }

    var isPaused: Bool { resumeTarget != nil }

    private func storePause(resume: Double?, pause: Double?) {
        resumeTarget = resume
        pauseTarget = pause
        UserDefaults.standard.set(resume, forKey: "resumeTarget")
        UserDefaults.standard.set(pause, forKey: "pauseTarget")
    }

    /// Manté el termòstat encès però sense escalfar: baixa la consigna per sota de l'actual.
    func setPaused(_ paused: Bool) {
        if paused {
            guard let current else { return }
            let low = max(minTarget, ((current - 1) * 2).rounded(.down) / 2)
            storePause(resume: target, pause: low)
            target = low
        } else if let resume = resumeTarget {
            storePause(resume: nil, pause: nil)
            target = resume
        }
        sendTarget()
    }

    func setTarget(_ value: Double) {
        storePause(resume: nil, pause: nil)
        target = min(maxTarget, max(minTarget, value))
        sendTarget()
    }

    func adjustTarget(by delta: Double) {
        storePause(resume: nil, pause: nil)
        target = min(maxTarget, max(minTarget, target + delta))
        sendTarget(after: .milliseconds(800))
    }

    private func sendTarget(after delay: Duration = .zero) {
        pendingTarget?.cancel()
        let value = Int((target * 10).rounded())
        pendingTarget = Task {
            try? await Task.sleep(for: delay)
            guard !Task.isCancelled else { return }
            do {
                try await client?.send(code: "temp_set", value: value)
                clearError()
            } catch {
                report(error)
            }
            pendingTarget = nil
        }
    }
}
