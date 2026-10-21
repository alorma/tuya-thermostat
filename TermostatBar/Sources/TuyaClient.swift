import CryptoKit
import Foundation

struct Config: Codable {
    var accessId: String
    var accessSecret: String
    var region: String
    var deviceId: String

    static var url: URL {
        FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("Library/Application Support/TermostatBar/config.json")
    }

    static func load() throws -> Config {
        try JSONDecoder().decode(Config.self, from: Data(contentsOf: url))
    }
}

enum TuyaError: LocalizedError {
    case subscriptionExpired
    case notAuthorized
    case invalidCredentials
    case api(String)
    case malformed

    static let consoleURL = URL(string: "https://iot.tuya.com")!

    static func from(code: Int, message: String) -> TuyaError {
        if code == 28841002 || message.lowercased().contains("expire") { return .subscriptionExpired }
        if [1106, 28841001, 28841105].contains(code) { return .notAuthorized }
        if [1001, 1002, 1004].contains(code) { return .invalidCredentials }
        return .api("Tuya (\(code)): \(message)")
    }

    var errorDescription: String? {
        switch self {
        case .subscriptionExpired: return "La subscripció d'IoT Core de Tuya ha caducat"
        case .notAuthorized: return "Tuya no et deixa accedir al termòstat"
        case .invalidCredentials: return "Les credencials de Tuya no són vàlides"
        case .api(let message): return message
        case .malformed: return "Resposta inesperada de Tuya"
        }
    }

    var instructions: String? {
        switch self {
        case .subscriptionExpired:
            return "1. Entra a iot.tuya.com\n2. Cloud → Development → el teu projecte → Service API\n3. A IoT Core, renova o amplia el període de prova (és gratuït)\n4. Torna aquí i prem refrescar"
        case .notAuthorized:
            return "1. Entra a iot.tuya.com → el teu projecte → Devices\n2. Comprova que el compte de l'app Tuya Smart segueix vinculat (Link App Account); si no, escaneja el QR de nou\n3. A Service API, comprova que IoT Core i Authorization Token Management estan actius"
        case .invalidCredentials:
            return "Copia de nou l'Access ID i l'Access Secret des de iot.tuya.com → el teu projecte → Overview a ~/Library/Application Support/TermostatBar/config.json i reinicia l'app."
        default:
            return nil
        }
    }
}

struct ThermostatStatus {
    var isOn = false
    var current: Double?
    var target: Double?
    var minTarget = 5.0
    var maxTarget = 35.0
    var battery: Int?
}

actor TuyaClient {
    private let config: Config
    private let base: URL
    private var token: String?
    private var tokenExpiry = Date.distantPast

    init(config: Config) {
        self.config = config
        self.base = URL(string: "https://openapi.tuya\(config.region).com")!
    }

    func status() async throws -> ThermostatStatus {
        let json = try await request("GET", "/v1.0/devices/\(config.deviceId)/status")
        guard let items = json["result"] as? [[String: Any]] else { throw TuyaError.malformed }
        var values: [String: Any] = [:]
        for item in items {
            if let code = item["code"] as? String { values[code] = item["value"] }
        }
        func degrees(_ code: String) -> Double? { (values[code] as? Int).map { Double($0) / 10 } }
        var result = ThermostatStatus()
        result.isOn = values["switch"] as? Bool ?? false
        result.current = degrees("temp_current")
        result.target = degrees("temp_set")
        result.minTarget = degrees("lower_temp") ?? 5
        result.maxTarget = degrees("upper_temp") ?? 35
        result.battery = values["battery_percentage"] as? Int
        return result
    }

    func send(code: String, value: Any) async throws {
        let body = try JSONSerialization.data(withJSONObject: ["commands": [["code": code, "value": value]]])
        let json = try await request("POST", "/v1.0/devices/\(config.deviceId)/commands", body: body)
        if json["result"] as? Bool != true { throw TuyaError.api("El termòstat ha rebutjat l'ordre") }
    }

    private func ensureToken() async throws {
        if token != nil, tokenExpiry.timeIntervalSinceNow > 60 { return }
        token = nil
        let json = try await request("GET", "/v1.0/token?grant_type=1", authenticated: false)
        guard let result = json["result"] as? [String: Any],
              let accessToken = result["access_token"] as? String,
              let expire = result["expire_time"] as? Double
        else { throw TuyaError.malformed }
        token = accessToken
        tokenExpiry = Date().addingTimeInterval(expire)
    }

    private func request(
        _ method: String, _ path: String, body: Data? = nil, authenticated: Bool = true
    ) async throws -> [String: Any] {
        if authenticated { try await ensureToken() }

        let timestamp = String(Int(Date().timeIntervalSince1970 * 1000))
        let bodyHash = SHA256.hash(data: body ?? Data()).hex(uppercase: false)
        let stringToSign = "\(method)\n\(bodyHash)\n\n\(path)"
        let accessToken = authenticated ? (token ?? "") : ""
        let signature = sign(config.accessId + accessToken + timestamp + stringToSign)

        var urlRequest = URLRequest(url: URL(string: base.absoluteString + path)!)
        urlRequest.httpMethod = method
        urlRequest.httpBody = body
        urlRequest.timeoutInterval = 15
        urlRequest.setValue(config.accessId, forHTTPHeaderField: "client_id")
        urlRequest.setValue(signature, forHTTPHeaderField: "sign")
        urlRequest.setValue(timestamp, forHTTPHeaderField: "t")
        urlRequest.setValue("HMAC-SHA256", forHTTPHeaderField: "sign_method")
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if authenticated { urlRequest.setValue(accessToken, forHTTPHeaderField: "access_token") }

        let (data, _) = try await URLSession.shared.data(for: urlRequest)
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw TuyaError.malformed
        }
        if json["success"] as? Bool != true {
            let code = json["code"] as? Int ?? 0
            let message = json["msg"] as? String ?? "Error desconegut"
            if code == 1010 { token = nil }
            throw TuyaError.from(code: code, message: message)
        }
        return json
    }

    private func sign(_ message: String) -> String {
        let key = SymmetricKey(data: Data(config.accessSecret.utf8))
        return HMAC<SHA256>.authenticationCode(for: Data(message.utf8), using: key).hex(uppercase: true)
    }
}

private extension Sequence where Element == UInt8 {
    func hex(uppercase: Bool) -> String {
        map { String(format: uppercase ? "%02X" : "%02x", $0) }.joined()
    }
}
