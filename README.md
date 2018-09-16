# ifly Nation Manager #

## Summary ##
ifly Nation Manager is a free and platform-independent client for NationStates's Telegram API which provides an easy and simple way to manage large numbers of puppets. This program uses parts of my [JavaTelegram](https://github.com/iFlyCode/NationStates-JavaTelegram) library to interface with and provide appropriate connections to the NationStates API.

This program also uses some publicly available encryption libraries to encrypt and decrypt your passwords. The password to the nation itself is stored in a symmetric cipher. The master password is used to encrypt that information, and is kept in a one-way cipher. Strength could be increased by inclusion of the Java cryptographic extensions, but as not everyone will have those installed, it is not.

Since everything is encrypted, do try to remember your master password. If you don't, it will break and you will be out of luck. The program allows you to delete old files which are encrypted with a password you don't know. But you will delete that data. You must have the master password with which that file was created if you want to access your information.

## Documentation ##

### System Requirements ###
* Java JRE 8 (https://java.com/en/download/)
* A [NationStates](http://www.nationstates.net) nation

### GUI notes ###

When the program starts, a screen pops up asking for a master password. This is the password by which your nation passwords will be encrypted. Remember this password. I recommend one which is complicated. You will not be able to access the program if you fail the password check.

You can import data files, but to use them, you must have a copy of the Salt by which they were encrypted. Otherwise, it will not be able to decrypt your passwords. To use the program, you must have the appropriate Salt with your master password.

There are four buttons at the bottom of the list of all the nations. 

1. `+` pulls up a prompt to add nations. 

2. `-` removes those nations. 

3. `Show` opens those nations in the browser. 

4. `i` opens those nations for an at-a-glance view in an Inspector.

### CLI notes ###

You can access this program from the CLI. You do not need to download a different executable. The program will seamlessly transition between operation methods. However, the version with the CLI is more limited. It can do all the things you expect it to do, but you may have to copy over master-password-compatible files after they are created.

Using CLI though, means that it is quite easy to set up some kind of `cron` script to keep all of your nations alive indefinitely. 