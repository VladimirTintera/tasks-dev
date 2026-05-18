//
//  AppDelegate.swift
//  iosApp
//
//  Created by Vladimír Tintěra on 02.04.2026.
//

import Foundation
import UIKit
import Shared

class AppDelegate : NSObject, UIApplicationDelegate {
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        
        InitializeKt.initialize()
        
        return true
    }
}
