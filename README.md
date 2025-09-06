C.L.A.R.A — Cheat.Limiting.Adaptive.Response.Algorithm (Beta)

[![Build Status](https://github.com/tomasalias/C.L.A.R.A/actions/workflows/build.yml/badge.svg)](https://github.com/tomasalias/C.L.A.R.A/actions/workflows/build.yml)
[![Gemini AI Integration](https://github.com/tomasalias/C.L.A.R.A/actions/workflows/gemini-test.yml/badge.svg)](https://github.com/tomasalias/C.L.A.R.A/actions/workflows/gemini-test.yml)

C.L.A.R.A is one of the first AI-assisted anti-cheat systems for Minecraft. Our advanced AI integration using Google's **Gemini 2.5 Flash Lite** provides intelligent behavior analysis and enhanced cheat detection capabilities, reducing false positives while improving accuracy.

## 🚀 AI-Enhanced Features

**NEW: Gemini 2.5 Flash Lite Integration**
- **Optimized AI Model**: Using Google's latest Gemini 2.5 Flash Lite for faster, more efficient analysis
- **Intelligent Analysis**: AI-powered behavior pattern recognition
- **Reduced False Positives**: Smart distinction between lag and cheats
- **Contextual Decisions**: Considers server performance, player ping, and environmental factors
- **Advanced Prompting**: Specialized prompts designed for Minecraft cheat detection
- **Configurable Actions**: Flexible response system based on AI confidence levels

## Current Features
C.L.A.R.A monitors key client packets to detect values and behaviors that are impossible in vanilla survival Minecraft. All checks are designed to be efficient, asynchronous, and explicitly rule-based.

Monitored Packets:
- Animation
- Abilities (basic anti-fly check)
- Position
- PositionLook
- Look
- BlockDig
- BlockPlace
- Chat (basic moderation system)
- HeldItemSlot
- InteractEntity
- SteerVehicle
- VehicleMove
- LoginStart

Note: LoginStart includes functionality from our other plugin, DDosDefender. Due to differences in packet management libraries, customization and client messaging are currently limited but planned for future updates.

## 🤖 AI Integration

**Gemini AI Analysis**: Each check can now be enhanced with Google's Gemini AI for:
- **Pattern Recognition**: Detects complex cheating patterns over time
- **Context Awareness**: Understands server lag, player ping, and environmental factors  
- **Intelligent Decisions**: Reduces false positives through advanced behavior analysis
- **Adaptive Responses**: Actions based on AI confidence levels (warn/kick/ban/monitor)

**Commands**:
- `/ai status` - View AI system status
- `/ai analyze <player>` - Perform AI analysis on player
- `/ai test <player>` - Test AI system with sample data
- `/ai testconnection` - Verify Gemini API connection

See [AI Integration Guide](AI_INTEGRATION_GUIDE.md) for detailed setup and configuration.

Experimental: TimerCheckA
Added in version A-0.2 and above, TimerCheckA uses the PositionLook packet and player ping to estimate packet timing consistency. Since Minecraft clients typically send movement packets every 50ms (20 TPS), this check flags packets arriving faster than 49.5ms, which may indicate timer manipulation.
Planned improvements:
- Incorporating Position and Look packets
- Adaptive thresholds based on server tick rate

Note: C.L.A.R.A is currently in Alpha. Bugs and incomplete features may be present. We’re actively developing new detection logic, improving client messaging, and expanding packet coverage. Feedback is welcome and appreciated as the project evolves.

## 📋 Quick Setup

1. **Download** the latest release
2. **Install** in your server's plugins folder
3. **Configure** your Gemini AI API key in `config.yml`
4. **Restart** your server
5. **Test** with `/ai status` to verify AI integration

**Requirements**:
- Java 21+
- Spigot/Paper 1.20.4+
- Gemini AI API key (free tier available)
- Internet connection for AI features
