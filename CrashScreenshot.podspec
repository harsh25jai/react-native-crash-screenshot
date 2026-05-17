require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "CrashScreenshot"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors = {
    "Contributors" => package["homepage"]
  }

  s.platforms    = { :ios => min_ios_version_supported }
  s.source       = { :git => ".git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift,cpp}"
  s.private_header_files = "ios/**/*.h"

  if respond_to?(:install_modules_dependencies, true)
    install_modules_dependencies(s)
  end
end
