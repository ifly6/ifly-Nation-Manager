# ifly Nation Manager #

## Summary ##
ifly Nation Manager is a free and platform-independent client for NationStates's Telegram API which provides an easy and simple way to manage large numbers of puppets. This program uses parts of my [JavaTelegram](https://github.com/iFlyCode/NationStates-JavaTelegram) library to interface with and provide appropriate connections to the NationStates API.

It is *your* responsibility to know how the telegram API works. Use of this program is agreement that you understand those limitations and requirements. It also agrees that you cannot claim losses, damages, or other negative effects from the author if action is taken against your NationStates account.

## Documentation ##

### System Requirements ###
* Java JRE 8 (https://java.com/en/download/)
* A [NationStates](http://www.nationstates.net) nation
* A NationStates [API client key](http://www.nationstates.net/pages/api.html#telegrams)
* A NationStates [telegram ID and secret key](http://www.nationstates.net/pages/api.html#telegrams)

### Operation ###

When the program starts, a screen pops up asking for a master password. This is the password by which your nation passwords will be encrypted. Remember this password. I recommend one which is complicated. You will not be able to access the program if you fail the password check.

You can get around this by deleting the `hash-store` file in the appropriate Application Support folder. However, without knowledge of the password itself, you cannot access any of your nations, as each nation's password is also encrypted by the master password provided at the start.

You can import data files, but to use them, you must have a copy of the Salt by which they were encrypted. Otherwise, it will not be able to decrypt your passwords. To use the program, you must have the appropriate Salt with your master password.

There are four buttons at the bottom of the list of all the nations. 

1. `+` pulls up a prompt to add nations. 

2. `-` removes those nations. 

3. `Show` opens those nations in the browser. 

4. `i` opens those nations for an at-a-glance view in an Inspector.
