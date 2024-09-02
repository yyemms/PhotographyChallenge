package com.example.photographychallenge_project

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import com.example.photographychallenge_project.ui.theme.PhotographyChallengeProjectTheme
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        setContent {
            PhotographyChallengeProjectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF7DA6F)
                ) {
                    var currentScreen by remember { mutableStateOf("home") }

                    Column {
                        NavigationBar(currentScreen = currentScreen, onNavigate = { screen -> currentScreen = screen })
                        when (currentScreen) {
                            "home" -> HomeScreen(onNavigate = { screen -> currentScreen = screen })
                            "login" -> LoginScreen(onNavigate = { screen -> currentScreen = screen })
                            "register" -> RegisterScreen(onNavigate = { screen -> currentScreen = screen })
                            "challenge" -> ChallengeScreen(onNavigate = { screen -> currentScreen = screen })
                            "gallery" -> ChallengeGalleryScreen(onNavigate = { screen -> currentScreen = screen })
                            "logout" -> LogOutScreen(onNavigate = { screen -> currentScreen = screen })
                        }
                    }
                }
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("FirebaseAuth", "User already signed in: ${currentUser.email}")
        }
    }
    @Composable
    fun NavigationBar(currentScreen: String, onNavigate: (String) -> Unit) {
        val isUserLoggedIn = auth.currentUser != null

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .height(60.dp)
                .padding(vertical = 2.dp)
                .background(Color.Black),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf("home", "challenge") +
                    if (isUserLoggedIn) listOf( "gallery", "logout") else listOf("login")
            val displayNames = listOf("Home", "Challenge") +
                    if (isUserLoggedIn) listOf( "Challenge\nGallery", "Log out") else listOf("Log in")

            navItems.forEachIndexed { index, screen ->
                Button(
                    onClick = { onNavigate(screen) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (currentScreen == screen) Color.Yellow else Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = displayNames[index],
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }

    @Composable
    fun HomeScreen(onNavigate: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(300.dp)
            )

            Text(
                text = "\n\tPhotography \n\n \tChallenge\n  ",
                fontSize = 40.sp,
                modifier = Modifier
                    .padding(8.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Looking for inspiration to ignite\nyour passion for photography?\nLook no further!\n\nOur app provides daily challenges that push your creativity and help you see the world from a new perspective.",
                fontSize = 20.sp,
                modifier = Modifier
                    .padding(8.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))


        }
    }

    @Composable
    fun LoginScreen(onNavigate: (String) -> Unit) {
        val email = remember { mutableStateOf("") }
        val password = remember { mutableStateOf("") }

        fun signInUser(email: String, password: String) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("FirebaseAuth", "signInWithEmail:success")
                        val user = auth.currentUser
                        onNavigate("challenge")
                    } else {
                        Log.w("FirebaseAuth", "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Login", fontSize = 32.sp, color = Color.Black)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                label = { Text("Email") },
                modifier = Modifier.width(300.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.width(300.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    Log.d("FirebaseAuth", "Login button clicked")
                    signInUser(email.value, password.value)
                },
                modifier = Modifier.width(200.dp),
                colors = ButtonDefaults.buttonColors(Color.Black)
            ) {
                Text(text = "Login", fontSize = 20.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    Log.d("FirebaseAuth", "Navigating to RegisterScreen")
                    onNavigate("register")
                },
                modifier = Modifier.width(200.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFFF0B513))
            ) {
                Text(text = "Register", fontSize = 20.sp, color = Color.Black)
            }
        }
    }

    @Composable
    fun RegisterScreen(onNavigate: (String) -> Unit) {
        val email = remember { mutableStateOf("") }
        val password = remember { mutableStateOf("") }


        fun createUser(email: String, password: String) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("FirebaseAuth", "createUserWithEmail:success")
                        val user = auth.currentUser
                        onNavigate("login")
                    } else {
                        Log.w("FirebaseAuth", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Registration failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Register",
                fontSize = 32.sp,
                color = Color.Black,
                modifier = Modifier.padding(16.dp)
            )

            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                label = { Text("Email") },
                modifier = Modifier
                    .width(300.dp)
                    .padding(vertical = 8.dp)
            )

            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .width(300.dp)
                    .padding(vertical = 8.dp)
            )

            Button(
                onClick = {
                    Log.d("FirebaseAuth", "Register button clicked")
                    createUser(email.value, password.value)
                },
                modifier = Modifier
                    .padding(8.dp)
                    .width(200.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFFF0B513))
            ) {
                Text(
                    text = "Register",
                    fontSize = 20.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    Log.d("FirebaseAuth", "Navigating to LoginScreen")
                    onNavigate("login")
                },
                modifier = Modifier
                    .padding(8.dp)
                    .width(200.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(Color.Black)
            ) {
                Text(
                    text = "Login",
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }


    @Composable
    fun ChallengeScreen(onNavigate: (String) -> Unit) {
        val context = LocalContext.current
        val storage = Firebase.storage
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        var imageName by remember { mutableStateOf("") }
        var challengeText by remember { mutableStateOf("Shake your phone to get a new challenge!") }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        DisposableEffect(Unit) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val acceleration = sqrt(
                        event.values[0].toDouble().pow(2.0) +
                                event.values[1].toDouble().pow(2.0) +
                                event.values[2].toDouble().pow(2.0)
                    ).toFloat()

                    val threshold = 20f
                    if (acceleration > threshold) {
                        fetchChallenge { challenge ->
                            challengeText = challenge ?: "No challenge found"
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }

        val pickImageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri: Uri? ->
                imageUri = uri
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = challengeText,
                fontSize = 32.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = imageName,
                onValueChange = { imageName = it },
                label = { Text("Enter Image Name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Button(
                onClick = { pickImageLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(Color.Black)
            ) {
                Text(text = "Select Image", fontSize = 20.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (imageUri != null && imageName.isNotEmpty()) {
                        uploadImageToFirebase(storage, imageUri!!, imageName, context)
                    } else {
                        Toast.makeText(context, "Please select an image and enter a name.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(Color.Black)
            ) {
                Text(text = "Upload Image", fontSize = 20.sp, color = Color.White)
            }
        }
    }

    private fun uploadImageToFirebase(storage: FirebaseStorage, imageUri: Uri, imageName: String, context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: return

        val storageRef = storage.reference
        val imagesRef = storageRef.child("images/$uid-$imageName.jpg")

        imagesRef.putFile(imageUri)
            .addOnSuccessListener {
                Toast.makeText(context, "Image Uploaded Successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
            }
    }






    @Composable
    fun LogOutScreen(onNavigate: (String) -> Unit) {
        FirebaseAuth.getInstance().signOut()
        Log.d("FirebaseAuth", "User signed out")
        onNavigate("home")
    }

    fun fetchChallenge(onChallengeFetched: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("challenges")
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val challengeList = result.documents.map { it.getString("name") }
                    val randomChallenge = challengeList.randomOrNull()
                    onChallengeFetched(randomChallenge)
                } else {
                    onChallengeFetched(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firebase", "Error getting documents.", exception)
                onChallengeFetched(null)
            }
    }

    @Composable
    fun ChallengeGalleryScreen(onNavigate: (String) -> Unit) {
        val storage = Firebase.storage
        var imageList by remember { mutableStateOf<List<ImageData>>(emptyList()) }

        LaunchedEffect(Unit) {
            fetchImageUrls(storage) { images ->
                imageList = images
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Challenge gallery",
                fontSize = 32.sp,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                color = Color.Black
            )

            if (imageList.isEmpty()) {
                Text(
                    text = "No images uploaded yet. Please upload an image.",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(imageList) { imageData ->
                        Text(text = "Challenge: ${imageData.name}", fontSize = 24.sp, color = Color.Black)
                        AsyncImage(
                            model = imageData.url,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    data class ImageData(val name: String, val url: String)

    private fun fetchImageUrls(storage: FirebaseStorage, onFetched: (List<ImageData>) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: return

        val storageRef = storage.reference.child("images/")
        storageRef.listAll()
            .addOnSuccessListener { result ->
                val userItems = result.items.filter { it.name.startsWith(uid) }
                val urlTasks = userItems.map { item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
                        ImageData(item.name.removePrefix("$uid-").removeSuffix(".jpg"), uri.toString())
                    }
                }

                Tasks.whenAllSuccess<Uri>(userItems.map { it.downloadUrl })
                    .addOnSuccessListener { uris ->
                        val imageDataList = uris.mapIndexed { index, uri ->
                            ImageData(userItems[index].name.removePrefix("$uid-").removeSuffix(".jpg"), uri.toString())
                        }
                        onFetched(imageDataList)
                    }
                    .addOnFailureListener {
                        onFetched(emptyList())
                    }
            }
            .addOnFailureListener {
                onFetched(emptyList())
            }
    }



}
