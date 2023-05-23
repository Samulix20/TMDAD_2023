'use strict';

// Login page
const usernamePage = document.querySelector('#username-page');
const usernameForm = document.querySelector('#usernameForm');
const loginInfoElement = document.querySelector('#logininfo');

// Chat page
const chatPage = document.querySelector('#chat-page');

// Area for showing msgs
const inboxName = document.querySelector('#inboxName');
const messageArea = document.querySelector('#messageArea');
const messageInput = document.querySelector('#message');
const connectingElement = document.querySelector('#connecting');

// Form for starting chats
const chatStartForm = document.querySelector('#chatStartForm');

// Group operation from
const groupOperationForm = document.querySelector('#groupOperationForm');

// Notification
const notificationContainer = document.querySelector('#notifContainer');
const notificationText = document.querySelector('#notif');

// Logout form
const logoutForm = document.querySelector('#logoutForm');

// Cached files
const fileCache = new Map();

let shownUUID = []
let stompClient = null;

const colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

// Auto login
let username = window.localStorage.getItem("username");
let jwtToken = window.localStorage.getItem("jwt");
if(username && jwtToken) {
    fetch('/api/users/secured', {
        headers: {'Authorization': 'Bearer ' + jwtToken}
    })
    .then((r) => {
        if(r.ok) {
            startChatPage();
        }
    });
}

function roomMessageFetch() {
    // Auto group message fetch
    let roomRE = new RegExp("/rooms/(.*)");
    let REeval = roomRE.exec(window.location.pathname);
    if(REeval === null) return;

    let msg = {
        target: REeval[1]
    }
    stompClient.send("/app/group.messages", {}, JSON.stringify(msg));
}

function loginError(e, info) {
    if(e) console.log(e);
    loginInfoElement.textContent = info
    loginInfoElement.style.color = 'red';
    loginInfoElement.style.backgroundColor = '#FFCCCB'
    loginInfoElement.classList.remove('hidden');
}

function startChatPage() {
    inboxName.innerHTML = username + "'s Inbox"
    usernamePage.classList.add('hidden');
    chatPage.classList.remove('hidden');
    const socket = new SockJS('/api/websocket');
    stompClient = Stomp.over(socket);
    var stompHeaders = {};
    stompHeaders['token'] = jwtToken;
    stompClient.connect(stompHeaders, onConnected, onError);
}

function login(event) {
    username = document.querySelector('#name').value.trim();
    let password = document.querySelector('#password').value.trim();
    event.preventDefault();

    if(username && password) {
        let jsonRequestBody = JSON.stringify({
            username: username,
            password: password,
        });
    
        let endpoint = null
        let errStr = null
    
        if (event.submitter.id == 'register') {
            endpoint = '/api/users/register'
            errStr = 'Register'
        } else {
            endpoint = '/api/users/login'
            errStr = 'Login'
        }
        
        fetch(endpoint, {
            method: 'POST',
            mode: 'cors',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
            },
            body: jsonRequestBody
        })
        .then(x => x.json())
        .then(data => {
            jwtToken = data.token;
            window.localStorage.setItem("jwt", jwtToken);
            window.localStorage.setItem("username", username);
            startChatPage();
        }).catch (e => {
            loginError(e, errStr + " error");
        });
    } else {
        loginError(null, "Missing Fields");
    }

    usernameForm.reset();
}

function logout(event) {
    event.preventDefault();
    window.localStorage.removeItem("username");
    window.localStorage.removeItem("jwt");
    username = null;
    jwtToken = null;
    window.location.reload();
}

async function onConnected() {
    // Subscribe to the user topic
    await stompClient.subscribe('/topic/chat/' + username, onMessageReceived);
    // Subscribe to the system personal notifications topic
    await stompClient.subscribe('/topic/system/notifications/' + username, onNotificationReceived);
    // Subscribe to the system trends
    await stompClient.subscribe('/topic/system/trends', onNotificationReceived);
    connectingElement.classList.add('hidden');
    stompClient.send("/app/chat.start", {}, "");
    roomMessageFetch();
}

function startChat(event) {
    event.preventDefault();

    let msgReceiver = document.querySelector('#userStart').value.trim();
    let msgContent = document.querySelector('#messageStart').value.trim();
    let msgAttch = document.querySelector('#fileAttch').files;
    let msgReceiverType = document.querySelector('#sendRcvType').value;
    let msg;

    var msgUUID = crypto.randomUUID()

    if(msgReceiver && stompClient) {

        let tg = msgReceiverType === 'group';
        let ts = (new Date(Date.now())).toISOString();

        if(msgAttch.length > 0) {
            let attchUUID;
            for (var i = 0; i < msgAttch.length; i++) {
                
                // 20 MB Limit
                if(msgAttch[i].size > 20971520) {
                    console.error("SIZE TOO BIG");
                    return;
                }

                // Add file type extension to directly download using the attchUUID
                attchUUID = crypto.randomUUID() + '.' + msgAttch[i].name.split('.').pop()
                fileCache.set(attchUUID, msgAttch[i]);
            }
            msg = {
                uuid: msgUUID,
                content: attchUUID,
                toGroup: tg,
                receiver: msgReceiver,
                timestamp: ts,
                type: 'ATTACHMENT'
            }
        } else if (msgContent) {
            msg = {
                uuid: msgUUID,
                content: msgContent,
                toGroup: tg,
                receiver: msgReceiver,
                timestamp: ts,
                type: 'CHAT'
            }
        } else {
            return;
        }

        stompClient.send("/app/chat.send", {}, JSON.stringify(msg));
        chatStartForm.reset();
    }
}

function groupOperation(event) {
    event.preventDefault();

    let targetGroup = document.querySelector('#targetGroup').value.trim();
    let targetUser = document.querySelector('#targetUser').value.trim();
    let opType = document.querySelector('#groupOpType').value;

    switch(opType) {
        case 'createGroup':
            if(targetGroup) {
                let msg = {
                    target: targetGroup
                }
                stompClient.send("/app/group.create", {}, JSON.stringify(msg));
            }
            break;
        case 'deleteGroup':
            if(targetGroup) {
                let msg = {
                    target: targetGroup
                }
                stompClient.send("/app/group.delete", {}, JSON.stringify(msg));
            }
            break;
        case 'addToGroup':
            if(targetGroup && targetUser) {
                let msg = {
                    target: targetGroup,
                    name: targetUser
                }
                stompClient.send("/app/group.addUser", {}, JSON.stringify(msg));
            }
            break;
        case 'removeFromGroup':
            if(targetGroup && targetUser) {
                let msg = {
                    target: targetGroup,
                    name: targetUser
                }
                stompClient.send("/app/group.removeUser", {}, JSON.stringify(msg));
            }
            break;
    }

    groupOperationForm.reset();
}

function onError(error) {
    try {
        console.error('Error:', error.headers.message);
    } catch (e) {}
    window.location.reload();
}

function onNotificationReceived(payload) {
    let notification = JSON.parse(payload.body);
    notificationContainer.style.color = '';
    notificationContainer.style.backgroundColor = '';
    notificationText.textContent = '';

    if (notification.type === 'UPLOAD_FILE') {
        fetch('/minio' + notification.url, {
            method: 'PUT',
            body: fileCache.get(notification.uuid)
        }).catch((e) => {
            console.log(e);
        });
        fileCache.delete(notification.uuid);
    } else if (notification.type === 'MESSAGE_LIST') {
        notification.messages.forEach(
            m => {
                displayChatMessage(m);
            }
        );
    } else if (notification.type === 'TREND_LIST') {
        let printmsg = "TRENDING: ";
        let c = 0;
        notification.trends.forEach(
            t => {
                printmsg = printmsg + t.first + ' | ';
                c = c + 1;
            }
        );
        if(c > 0) notificationText.textContent = printmsg.substring(0, printmsg.length - 3);
    } else if (notification.type === 'ERROR') {
        notificationContainer.style.color = 'red';
        notificationContainer.style.backgroundColor = '#FFCCCB';
        notificationText.textContent = notification.info;
    } else {
        notificationText.textContent = notification.info;
    }
}

async function downloadFile(name) {
    const image = await fetch("/api/files/" + name, {
        mode: "cors",
        headers: {'Authorization': 'Bearer ' + jwtToken}
    });
    if (image.ok) {
        const imageBlob = await image.blob();
        const imageURL = URL.createObjectURL(imageBlob);
        const anchor = document.createElement("a");
        anchor.href = imageURL;
        anchor.download = name;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        URL.revokeObjectURL(imageURL);
    }
}

function displayChatMessage(message) {
    if(shownUUID.includes(message.uuid)) return;
    else shownUUID.push(message.uuid);

    const messageElement = document.createElement('li');

    if(message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' joined!';
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' left!';
    } else {
        messageElement.classList.add('chat-message');

        const avatarElement = document.createElement('i');
        const avatarText = document.createTextNode(message.sender[0]);
        avatarElement.appendChild(avatarText);
        avatarElement.style['background-color'] = getAvatarColor(message.sender);

        messageElement.appendChild(avatarElement);

        const usernameElement = document.createElement('span');
        const usernameText = document.createTextNode(message.sender);
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);

        // Receiver print
        const receiverElement = document.createElement('span');
        receiverElement.setAttribute(
            "style", 
            "color: #777; font-weight: normal; font-style: italic;"
        );
        let t = ""
        // Direct msg
        if(message.receiver === username) {
            // pass
        }
        else t = " sent to " + message.receiver;
        receiverElement.appendChild(document.createTextNode(t));
        messageElement.appendChild(receiverElement);
        
        // Timestamp
        const timestampElement = document.createElement('span');
        timestampElement.setAttribute(
            "style", 
            "color: #777; font-weight: bold;"
        );
        let timestamp =  new Date(message.timestamp);
        timestampElement.appendChild(document.createTextNode(
            " " + timestamp.toLocaleTimeString() + 
            " " + timestamp.toLocaleDateString()
        ));
        messageElement.appendChild(timestampElement);
        
    }

    const textElement = document.createElement('p');

    if(message.type === 'ATTACHMENT') {
        var inputElement = document.createElement('button');
        inputElement.innerHTML = "Download File";
        inputElement.setAttribute("name", message.content);
        inputElement.addEventListener('click', function(event){
            downloadFile(event.target.getAttribute('name'));
        });
        textElement.appendChild(inputElement)

    } else {
        const messageText = document.createTextNode(message.content);
        textElement.appendChild(messageText);
    }
    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}

function onMessageReceived(payload) {
    displayChatMessage(JSON.parse(payload.body));
}

function getAvatarColor(messageSender) {
    let hash = 0;
    for (let i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }

    const index = Math.abs(hash % colors.length);
    return colors[index];
}

usernameForm.addEventListener('submit', login, true);
chatStartForm.addEventListener('submit', startChat, true);
groupOperationForm.addEventListener('submit', groupOperation, true);
logoutForm.addEventListener('submit', logout, true);
