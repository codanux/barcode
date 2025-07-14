import Foundation

@objc public class Barcode: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
