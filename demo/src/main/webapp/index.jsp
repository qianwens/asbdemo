<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>AppMod News Feed</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 800px;
            margin: 50px auto;
            padding: 20px;
            background-color: #f5f5f5;
        }
        
        h1 {
            color: #333;
            text-align: center;
        }
        
        .status {
            padding: 10px;
            margin: 20px 0;
            border-radius: 5px;
            text-align: center;
        }
        
        .connected {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        
        .disconnected {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        
        .news-container {
            background-color: white;
            border-radius: 8px;
            padding: 20px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            min-height: 200px;
        }
        
        .news-item {
            padding: 15px;
            margin: 10px 0;
            background-color: #e3f2fd;
            border-left: 4px solid #2196F3;
            border-radius: 4px;
            animation: fadeIn 0.5s;
        }
        
        .news-item .timestamp {
            color: #666;
            font-size: 0.85em;
            margin-top: 5px;
        }
        
        .news-item .message {
            font-size: 1.1em;
            color: #333;
            margin: 5px 0;
        }
        
        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(-10px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        
        .no-news {
            text-align: center;
            color: #999;
            padding: 40px;
        }
    </style>
</head>
<body>
    <h1>AppMod News Feed</h1>
    
    <div id="status" class="status disconnected">
        Connecting to server...
    </div>
    
    <div class="news-container">
        <div id="news-list">
            <div class="no-news">Waiting for news messages...</div>
        </div>
    </div>
    
    <script>
        let websocket = null;
        const statusDiv = document.getElementById('status');
        const newsList = document.getElementById('news-list');
        
        function connect() {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = protocol + '//' + window.location.host + '<%= request.getContextPath() %>/news-websocket';
            
            console.log('Connecting to WebSocket: ' + wsUrl);
            
            websocket = new WebSocket(wsUrl);
            
            websocket.onopen = function(event) {
                console.log('WebSocket connected');
                statusDiv.textContent = 'Connected to AppMod News Feed';
                statusDiv.className = 'status connected';
            };
            
            websocket.onmessage = function(event) {
                console.log('Received message: ' + event.data);
                addNewsItem(event.data);
            };
            
            websocket.onerror = function(event) {
                console.error('WebSocket error:', event);
                statusDiv.textContent = 'Connection error';
                statusDiv.className = 'status disconnected';
            };
            
            websocket.onclose = function(event) {
                console.log('WebSocket closed');
                statusDiv.textContent = 'Disconnected - Reconnecting...';
                statusDiv.className = 'status disconnected';
                
                // Attempt to reconnect after 3 seconds
                setTimeout(connect, 3000);
            };
        }
        
        function addNewsItem(message) {
            // Remove "no news" message if present
            const noNews = newsList.querySelector('.no-news');
            if (noNews) {
                noNews.remove();
            }
            
            // Create news item
            const newsItem = document.createElement('div');
            newsItem.className = 'news-item';
            
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message';
            messageDiv.textContent = message;
            
            const timestampDiv = document.createElement('div');
            timestampDiv.className = 'timestamp';
            timestampDiv.textContent = new Date().toLocaleString();
            
            newsItem.appendChild(messageDiv);
            newsItem.appendChild(timestampDiv);
            
            // Add to top of list
            newsList.insertBefore(newsItem, newsList.firstChild);
            
            // Keep only last 50 messages
            while (newsList.children.length > 50) {
                newsList.removeChild(newsList.lastChild);
            }
        }
        
        // Connect when page loads
        window.addEventListener('load', connect);
        
        // Close websocket when page unloads
        window.addEventListener('beforeunload', function() {
            if (websocket) {
                websocket.close();
            }
        });
    </script>
</body>
</html>
