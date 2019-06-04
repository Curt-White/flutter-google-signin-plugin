# Google Signin

This plugin is a modified version of the flutter Google Signin plugin with better support for the server side (Offline) access. In addition to this minor modification the ios code has been written in swift. This plugin is used for signing into a google account through the flutter application using native code and the google sign in packages for the two native platform. There is not yet a built in button for Flutter so the functions must be attached to some input device.

## Setup

#### IOS

First create an Ios OAuth 2.0 client by going to the Google Cloud Console and going to the APIs and Services tab. Then click create new credentials and follow the steps to create a new client. Click download .plist file and copy and paste the reversed client id in the XML string tags as shown below.

![alt test][plist]

Android

Create an Android client id for client side OAuth 2 flow. In order to use the Offline Access feature for a server side application, use the web client id that you use for your server side code.

## Configuration

The configuration for this package is the same as that of the react native google sign in package. Use the config object as shown in the example to configure the plugin.

The following options are available on the Config object:\
    **webClientId**: for web server authentication\
    **iosClientId**: (IOS only)\
    **offlineAccess**: for access to your account on the servers behalf\
    **hostedDomain**: a domain restriction\
    **loginHint**: (IOS only) prefilled email address
    **forceConsentPrompt**: (Android only) Ask for consent on every login\
    **accountName**: (Android only) specify account name to use\
    **scopes**: requested scopes




[plist]: ./docs/images/plist.png