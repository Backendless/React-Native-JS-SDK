require "json"

package = JSON.parse(File.read(File.join(File.dirname(__FILE__), "package.json")))

Pod::Spec.new do |s|
  # NPM package specification

  s.name           = 'RNBackendless'
  s.version        = package['version']
  s.summary        = package['description']
  s.description    = package['description']
  s.license        = package['license']
  s.author         = package['author']
  s.homepage       = package['homepage']

  s.source       = { :git => "https://github.com/Backendless/React-Native-JS-SDK", :tag => "v#{s.version}" }
  s.source_files = "ios/*.{h,m}"

  s.platform     = :ios, "10.0"

  s.dependency "React"
end
