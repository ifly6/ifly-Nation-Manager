# ifly Nation Manager #

## Summary ##
ifly Nation Manager is a free and platform-independent client for NationStates's Telegram API which provides an easy and simple way to manage large numbers of puppets. This program uses parts of [Communique](https://github.com/ifly6/Communique) library to interface with and connect to the NationStates API properly.

This program also uses some publicly available encryption libraries to encrypt and decrypt your passwords. The password to the nation itself is stored in a symmetric cipher. The master password is used to encrypt that information, and is kept in a one-way cipher. **Nb** the NationStates API requires that passwords be sent in clear-text, this is unavoidable. However, as connections use HTTPS, the impact of this is greatly diminished.

Since your passwords are encrypted, do try to remember your master password. If you don't, you will be out of luck. The program allows you to delete old files which are encrypted with a password you don't know. But you will delete that data. You must have the master password with which that file was created if you want to access your information.

## Documentation ##

### System Requirements ###
* Java JRE 8 (https://java.com/en/download/)
* A [NationStates](http://www.nationstates.net) nation

### GUI notes ###

When the program starts, a screen pops up asking for a master password. This is the password by which your nation passwords will be encrypted. Remember this password. I recommend one which is complicated. You will not be able to access the program if you fail the password check.

You can import data files, but to use them, you must have a copy of the salt by which they were encrypted. Otherwise, it will not be able to decrypt your passwords. To use the program, you must have the appropriate salt with your master password.

There are four buttons at the bottom of the list of all the nations. 

1. `+` pulls up a prompt to add nations. 

2. `-` removes those nations. 

3. `Show` opens those nations in the browser. 

4. `Connect` connects to the files with the API.
