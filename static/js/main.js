'use strict';

const backend_url = 'http://localhost:9090';
const wsEnpoint = '/websocket';

const usernamePage = document.querySelector('#username-page');
const chatPage = document.querySelector('#chat-page');
const usernameForm = document.querySelector('#usernameForm');
const messageForm = document.querySelector('#messageForm');
const messageInput = document.querySelector('#message');
const messageArea = document.querySelector('#messageArea');
const connectingElement = document.querySelector('#connecting');
const loginInfoElement = document.querySelector('#logininfo');

let stompClient = null;
let username = null;
let jwtToken = null;

const colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function loginError(e, info) {
    if(e) console.log(e);
    loginInfoElement.textContent = info
    loginInfoElement.style.color = 'red';
    loginInfoElement.style.backgroundColor = '#FFCCCB'
    loginInfoElement.classList.remove('hidden');
}

function register(event) {
    console.log('Reg');
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
            endpoint = '/users/register'
            errStr = 'Register'
        } else {
            endpoint = '/users/login'
            errStr = 'Login'
        }
        
        fetch(backend_url + endpoint, {
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
            usernamePage.classList.add('hidden');
            chatPage.classList.remove('hidden');
            const socket = new SockJS(backend_url + wsEnpoint);
            stompClient = Stomp.over(socket);
            var stompHeaders = {};
            stompHeaders['token'] = jwtToken;
            stompClient.connect(stompHeaders, onConnected, onError);
        }).catch (e => {
            loginError(e, errStr + " error");
        });
    } else {
        loginError(null, "Missing Fields");
    }

    usernameForm.reset();
}

async function onConnected() {
    // Subscribe to the Public Topic
    await stompClient.subscribe('/topic/public', onMessageReceived);

    // Tell your username to the server
    stompClient.send("/app/chat.register",
        {},
        JSON.stringify({sender: username, group: 'public', type: 'JOIN'})
    )

    connectingElement.classList.add('hidden');
}

function onError(error) {
    try {
        console.error('Error:', error.headers.message);
    } catch (e) {}
    
    connectingElement.textContent = 'No fue posible conectar con WebSocket';
    connectingElement.style.color = 'red';
}

function send(event) {
    const messageContent = messageInput.value.trim();

    if(messageContent && stompClient) {
        const chatMessage = {
            sender: username,
            content: messageInput.value,
            group: 'public',
            type: 'CHAT'
        };

        stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}

function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);

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
    }

    const textElement = document.createElement('p');
    const messageText = document.createTextNode(message.content);
    textElement.appendChild(messageText);

    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
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
messageForm.addEventListener('submit', send, true);


