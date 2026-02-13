#!/bin/bash
# Test Script - WebSocket Authentication với Token Expired

echo "🧪 Testing WebSocket Authentication Error Handling"
echo "=================================================="
echo ""

echo "📋 Test Cases:"
echo "1. ✅ Valid Token - Should connect successfully"
echo "2. ⏰ Expired Token - Should receive TOKEN_EXPIRED error"
echo "3. 🔧 Malformed Token - Should receive TOKEN_MALFORMED error"
echo "4. ❌ No Token - Should receive MISSING_TOKEN error"
echo ""

echo "🔍 Monitoring Server Logs..."
echo "Run this command in another terminal:"
echo "  tail -f logs/greenconnect-api.log | grep 'WebSocket Auth'"
echo ""

echo "📝 Expected Log Patterns:"
echo ""
echo "1. Token Expired:"
echo "   ⏰ [WebSocket Auth] Token EXPIRED - Issued at: ..., Expired at: ..."
echo "   💡 [WebSocket Auth] Client should refresh token and reconnect"
echo "   📤 [WebSocket Auth] Sending ERROR frame to client: TOKEN_EXPIRED"
echo ""

echo "2. Token Valid:"
echo "   ✅ [WebSocket Auth] Token validation PASSED"
echo "   👤 [WebSocket Auth] Authenticated user - ID: ..., Email: ..., Roles: ..."
echo "   ✅ [WebSocket Auth] SUCCESS - User xyz@example.com authenticated"
echo ""

echo "3. Token Malformed:"
echo "   🔧 [WebSocket Auth] Token MALFORMED: ..."
echo "   📤 [WebSocket Auth] Sending ERROR frame to client: TOKEN_MALFORMED"
echo ""

echo "4. No Token:"
echo "   ❌ [WebSocket Auth] No Authorization header or invalid format"
echo "   📤 [WebSocket Auth] Sending ERROR frame to client: MISSING_TOKEN"
echo ""

echo "================================"
echo "🎯 How to Test from Flutter App:"
echo "================================"
echo ""
echo "Test 1: Expired Token"
echo "  - Set JWT expiry to 1 minute in application.yml"
echo "  - Login and wait 2 minutes"
echo "  - Try to create a new conversation"
echo "  - Check if app auto-refreshes token and reconnects"
echo ""

echo "Test 2: Check Current Logs"
echo "  - Run: tail -n 100 logs/greenconnect-api.log | grep 'WebSocket Auth'"
echo ""

echo "✅ Backend is ready for testing!"
echo "Now implement the Flutter client according to WEBSOCKET_TOKEN_REFRESH_GUIDE.md"
