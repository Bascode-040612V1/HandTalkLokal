# Hand_Talk_Lokal - Sign Language Translator App

![Hand_Talk_Lokal Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

Hand_Talk_Lokal is an Android application that bridges communication gaps by translating sign language gestures into spoken language in real-time. The app empowers individuals who use sign language to communicate more effectively with those who don't understand sign language.

## 🌟 About Hand_Talk_Lokal

Hand_Talk_Lokal is designed to facilitate communication between sign language users and non-sign language users. By leveraging advanced computer vision and machine learning technologies, the app translates hand gestures into spoken words, breaking down communication barriers and promoting inclusivity.

## 🚀 Features

### 1. Sign Language to Speech Translator
This feature allows users to communicate using hand signs that are converted into text and spoken words.

#### How It Works:
1. The app uses the device's camera to detect and recognize hand gestures
2. When a hand sign is recognized, the app converts it into a corresponding word
3. The recognized word is displayed as a subtitle on the screen
4. Each new gesture adds to form a sentence
5. Previous sentences move upward and are stored in a scrollable history
6. The completed sentence is converted into speech output
7. All recognized text is saved in the app's data folder

#### Key Features:
- Real-time hand gesture recognition
- Subtitle display for each recognized gesture
- Automatic sentence formation
- Scrollable history of recognized sentences
- Speech output for translated text
- Multi-language support (English, Filipino, Hiligaynon, Cebuano, Maranao)
- Saved text data in the app's data folder

### 2. Basic Hand Sign Tutorials
This feature helps users learn hand signs step by step.

#### How It Works:
1. The tutorial section provides structured lessons that show users how to perform basic hand signs
2. Each lesson includes a visual demonstration, a text explanation, and optional voice guidance
3. The tutorials are grouped into categories such as letters, common words, and simple phrases to make learning easier

#### Key Features:
- Beginner-friendly tutorials
- Categories for easy navigation (Alphabet, Common Words, Basic Phrases, Emotions, Numbers)
- Visual hand sign demonstrations
- Text and voice explanations
- Swipe or tap navigation between lessons

## 👥 User Workflow

### General App Flow
1. User opens Hand_Talk_Lokal
2. Home screen is displayed with app description
3. User selects one of the two main features

### Sign Language to Speech Translator - Step-by-Step
1. User taps "Sign Language to Speech Translator"
2. The device camera turns on
3. User performs hand signs in front of the camera
4. The app recognizes the hand gesture
5. Recognized gesture appears as a subtitle
6. Each new gesture adds to form a sentence
7. Previous sentences move upward and are stored
8. User can scroll to view past recognized sentences
9. The completed sentence is converted into speech
10. All recognized text is saved in the app's data folder

### Basic Hand Sign Tutorials - Step-by-Step
1. User taps "Basic Hand Sign Tutorials"
2. Tutorial categories are displayed
3. User selects a category (Alphabet, Words, Phrases, etc.)
4. User selects a specific lesson
5. The app displays the hand sign demonstration
6. Text explains the meaning and usage of the sign
7. Optional voice guidance plays
8. User navigates to the next or previous lesson

## ♿ Accessibility & Ease of Use
- Large buttons for easy tapping
- Simple and clear layout
- High-contrast text for readability
- Minimal steps to complete tasks
- Voice-guided tutorials
- Multi-language support for regional inclusivity

## 🔧 Technical Implementation

### Frontend
- Built with Kotlin and Jetpack Compose
- Uses Material Design 3 for UI components
- Implements navigation between screens
- Integrates camera functionality with CameraX
- Responsive design for various screen sizes

### Backend
- Uses MediaPipe for hand tracking and gesture recognition
- Implements machine learning model for gesture classification
- Includes text-to-speech functionality with multi-language support
- Stores sentence history locally using DataStore
- ViewModel architecture for state management

### Permissions
- Camera permission for gesture recognition
- Internet permission for potential cloud services

## 🛠️ Tools & Technologies

| Category | Technology |
|---------|------------|
| **Language** | ![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white) |
| **Framework** | ![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white) |
| **UI** | ![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white) |
| **Computer Vision** | ![MediaPipe](https://img.shields.io/badge/MediaPipe-4285F4?style=for-the-badge&logo=google&logoColor=white) |
| **Camera** | ![CameraX](https://img.shields.io/badge/CameraX-4285F4?style=for-the-badge&logo=android&logoColor=white) |
| **Build Tool** | ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white) |
| **IDE** | ![Android Studio](https://img.shields.io/badge/Android_Studio-3DDC84?style=for-the-badge&logo=android-studio&logoColor=white) |

## 👨‍💻 Developed By

This application was developed by:
- **Bascode** - Lead Developer
- **Friends.inc** - Development Team

## 📁 Repository

GitHub: [@Bascode-040612V1/HandTalkLokal](https://github.com/Bascode-040612V1/HandTalkLokal)

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Kotlin 1.5+
- Android SDK API level 24+
- Physical device with camera (emulator has limited functionality)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Bascode-040612V1/HandTalkLokal.git
   ```
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Build and run the application

### Building the Project
```bash
./gradlew build
```

## 📱 Screenshots

*(Add screenshots of your app here)*

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Google's MediaPipe team for the excellent hand tracking technology
- The Android developer community for continuous support and inspiration
- All contributors who helped in testing and improving the application

## 📞 Support

For support, email bascode.developer@example.com or open an issue in the GitHub repository.

## 🔮 Future Enhancements
- Integration with more advanced gesture recognition models
- Cloud synchronization for user progress
- Social sharing features
- Offline mode for areas with limited connectivity
- Gesture learning mode for training the system with new signs
- Integration with sign language dictionaries
- Community-contributed gesture libraries