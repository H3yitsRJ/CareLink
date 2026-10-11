# CareLink

## Introduction

CareLink is an Android healthcare coordination application designed to help
individuals and caregivers organize important health-related information in
one convenient location. The goal of CareLink is to make managing medications,
appointments, care tasks, and health concerns easier and more accessible.

Managing healthcare information can become difficult when information is
spread across calendars, notes, medication bottles, and other applications.
CareLink provides a centralized application where users can keep track of
this information and quickly view what needs their attention.

CareLink is designed with simplicity and accessibility in mind. The interface
uses clear navigation, readable text, and easy-to-understand screens so that
users of different ages and technical experience levels can use the
application.



## Features

CareLink includes the following healthcare management features:

### User Accounts
- User registration and login with Firebase Authentication
- User profile creation and management
- Personalized dashboard with live user information
- Secure account logout

### Medication Management
- Add, edit, view, and delete medications
- Track medication names, dosages, and schedules
- Schedule medication reminders and notifications
- Record medication dose history
- Track medication refills and remaining quantities

### Appointment Management
- Add, edit, and view healthcare appointments
- View appointment details
- Track upcoming appointments

### Care Tasks and Health Concerns
- Create and manage care tasks
- Assign low, medium, or high task priorities
- Record and manage health concerns
- View important healthcare information

### Caregiver Access
- Connect caregivers with care recipients
- Manage caregiver access permissions
- Allow authorized caregivers to view patient information
- Support access to medications and health concerns

### Care History
- View recorded healthcare activities
- Track medication dose history
- View caregiver access activity
- Filter care history by activity type

### User Interface
- Navigation between major application screens
- Consistent CareLink colors and styling
- Readable text and accessible screen layouts
- User-friendly interface for patients and caregivers



## Technologies

CareLink is being developed using the following technologies:

- **Android Studio** - Primary development environment
- **Kotlin** - Programming language used for Android development
- **Jetpack Compose** - Used to build the application's user interface
- **Firebase** - Provides backend services for the application
- **Firebase Authentication** - Used for user account authentication
- **Cloud Firestore** - Used to store and retrieve application data
- **Git** - Used for version control
- **GitHub** - Used for source code management and team collaboration
- **Figma** - Used to create wireframes and design the user interface


## Installation

CareLink is currently under development and is not yet available through the
Google Play Store.

To run the current version of CareLink:

1. Install Android Studio.
2. Clone or download the CareLink GitHub repository.
3. Open Android Studio.
4. Select **Open** and choose the CareLink project folder.
5. Allow Gradle to synchronize and download the required dependencies.
6. Select an Android emulator or connect a compatible Android device.
7. Click **Run** in Android Studio.
8. CareLink should build and launch on the selected device.

Users installing a future release version will not need Android Studio.
Installation instructions will be updated when a distributable version of
the application becomes available.


## Development Setup

New developers working on CareLink should have the following installed:

- Android Studio
- Android SDK
- Git
- A GitHub account

### Clone the Repository

Clone the CareLink repository using the following command:

    git clone https://github.com/H3yitsRJ/CareLink.git

After cloning the repository, open the CareLink project folder in Android
Studio.

### Build the Project

After opening the project:

1. Allow Android Studio to complete the Gradle synchronization.
2. Install any requested Android SDK components.
3. Allow the required project dependencies to download.
4. Configure the Firebase connection if required.
5. Start an Android emulator or connect an Android device.
6. Select the device from Android Studio.
7. Click **Run** to compile and launch CareLink.

Developers should pull the latest version of the project from GitHub before
beginning new work. Completed changes should be pushed back to the repository
according to the team's Git workflow.


## License

CareLink is currently being developed as an educational capstone project.

The project is not currently distributed under an open-source license. A
license may be selected if CareLink is publicly distributed or developed
beyond the capstone project.


## Contributors

CareLink is being developed as a collaborative capstone project.

**Project Team:**

- Timothy Jackson
- Katie Lavely
- Robert Tobiasz
- RJ Vogel

Additional contributors will be added as development continues.



## Project Status

**Current Status: Alpha / Active Development**

CareLink is an Android healthcare coordination application
currently in the Alpha development stage.

The project has progressed beyond initial planning and design.
Core application screens have been implemented, and several
features are connected to Firebase services.

### Completed Development

- Created application wireframes and interface designs in Figma
- Established the Android Studio project and GitHub repository
- Integrated Firebase Authentication and Cloud Firestore
- Implemented user registration, login, and profile management
- Connected the dashboard to live user information
- Implemented medication management and reminders
- Added medication refill tracking
- Implemented appointment and care task management
- Added health concern tracking
- Implemented caregiver access and permission controls
- Added Care History records and filtering
- Improved application colors and interface consistency

### Current Development Focus

Development is focused on improving application reliability,
refining the user interface, and testing existing features.

The team continues to review Firebase permissions, improve
caregiver functionality, and resolve issues found during testing.

CareLink is not yet available as a public release.



## Known Issues

CareLink is currently in Alpha development. Although many
core features are functional, additional testing is required
before the application is ready for public release.

### Current Limitations

- Some application features require further testing.
- Caregiver permissions need additional security testing.
- Medication reminders require testing across Android versions.
- User interface consistency is still being refined.
- Application performance needs further evaluation.
- Some workflows may require additional error handling.

### Resolved Issues

- Resolved Firebase Firestore permission errors affecting
  caregiver access to patient information.
- Fixed medication saving and deletion issues.
- Corrected medication reminder scheduling problems.
- Improved caregiver selection and patient data access.
- Resolved issues with Care History data retrieval.
- Fixed several application build and navigation errors.

Additional issues discovered during development will be
documented and addressed before the final release.




## Roadmap

CareLink development is focused on completing testing,
improving existing features, and preparing the application
for its final release.

### Completed Milestones

- [x] Create application wireframes in Figma
- [x] Set up Android Studio and GitHub repository
- [x] Integrate Firebase Authentication
- [x] Connect application data to Cloud Firestore
- [x] Implement user registration and login
- [x] Create user profiles and dashboard
- [x] Implement medication management
- [x] Add medication reminders and dose tracking
- [x] Implement medication refill tracking
- [x] Implement appointment management
- [x] Implement care task management
- [x] Add health concern tracking
- [x] Implement caregiver access permissions
- [x] Add Care History records and filtering

### Upcoming Milestones

- [ ] Complete application-wide feature testing
- [ ] Verify caregiver permissions and security rules
- [ ] Test medication reminders on different devices
- [ ] Improve accessibility and screen consistency
- [ ] Add additional automated tests
- [ ] Improve error handling and user feedback
- [ ] Perform integration and usability testing
- [ ] Resolve remaining bugs
- [ ] Complete final application documentation
- [ ] Prepare CareLink for final release

The roadmap will be updated as development progresses
and additional features are completed.


## Support

CareLink is currently an educational capstone project and does not have a
public support system.

Issues discovered during development should be documented and tracked by the
CareLink development team through the project's development workflow.