# NetworkingProject
Chatting program with the following features: private chats, welcoming messages, and even the ability to choose from a list of songs and play it!

Special Commands- 

@[Name]: will send a private chat to the person you mentioned (users can now send the same private chat to multiple people by using multiple @[Name] tags)

/quit: this command will exit the chat  

"?": will show a list of songs that you can pick from to play

![song_name]: plays the song you choose only for you to hear  

/whoishere: shows an updated list of participants in the chat room

# Console Client 
- All network communication is now via serialized objects 
- Server broadcasts a special message with a list of all users in the chat room when a user leaves or a new user enters the room

# JavaFx Client 
- Uses a dialog box to ask the user for ip addres and port before connecting to server
- Once connected uses another dialog box to ask the user for their username and does not allow them to send messages until the username has been recieved 
- Has a box to enter chat messages and a display of the chatroom history
- Displays a list of all the current users that the server sends updates to


