package com.example.google_world_web.userdata

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.google_world_web.ui.theme.GoogleWorldWebTheme // Assuming your theme is here

enum class AuthMode {
    LOGIN,
    SIGNUP
}

@Composable
fun LoginPage(
    modifier: Modifier = Modifier,
    onLoginClicked: (email: String, pass: String) -> Unit,
    onSignUpClicked: (email: String, pass: String, confirmPass: String) -> Unit,
    // Optional: Add navigation for "Forgot Password" or other actions
    // onForgotPasswordClicked: () -> Unit
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    fun validateFields(): Boolean {
        emailError = if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Invalid email address" else null
        passwordError = if (password.isBlank() || password.length < 6) "Password must be at least 6 characters" else null

        confirmPasswordError = if (authMode == AuthMode.SIGNUP) {
            if (confirmPassword.isBlank()) "Please confirm your password" else if (password != confirmPassword) "Passwords do not match" else null
        } else {
            null // Not needed for login
        }
        return emailError == null && passwordError == null && (authMode == AuthMode.LOGIN || confirmPasswordError == null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (authMode == AuthMode.LOGIN) "Welcome Back!" else "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = if (authMode == AuthMode.LOGIN) "Login to continue" else "Sign up to get started",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = null },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            isError = emailError != null,
            singleLine = true
        )
        emailError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (authMode == AuthMode.LOGIN) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (authMode == AuthMode.LOGIN && validateFields()) onLoginClicked(email, password) }
            ),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, description)
                }
            },
            isError = passwordError != null,
            singleLine = true
        )
        passwordError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password Field (Only for SIGNUP mode)
        if (authMode == AuthMode.SIGNUP) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; confirmPasswordError = null },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (validateFields()) onSignUpClicked(email, password, confirmPassword) }
                ),
                trailingIcon = {
                    val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (confirmPasswordVisible) "Hide password" else "Show password"
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = image, description)
                    }
                },
                isError = confirmPasswordError != null,
                singleLine = true
            )
            confirmPasswordError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp)) // Smaller spacer for login mode
            // Optional: Forgot Password Text
            // Text(
            //     text = "Forgot Password?",
            //     modifier = Modifier
            //         .align(Alignment.End)
            //         .clickable { onForgotPasswordClicked() }
            //         .padding(vertical = 8.dp),
            //     color = MaterialTheme.colorScheme.primary,
            //     fontWeight = FontWeight.Medium
            // )
            // Spacer(modifier = Modifier.height(16.dp))
        }


        // Action Button (Login or Sign Up)
        Button(
            onClick = {
                focusManager.clearFocus() // Hide keyboard
                if (validateFields()) {
                    if (authMode == AuthMode.LOGIN) {
                        onLoginClicked(email, password)
                    } else {
                        onSignUpClicked(email, password, confirmPassword)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(if (authMode == AuthMode.LOGIN) "Login" else "Sign Up", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle Auth Mode
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (authMode == AuthMode.LOGIN) "Don't have an account?" else "Already have an account?",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (authMode == AuthMode.LOGIN) "Sign Up" else "Login",
                modifier = Modifier.clickable {
                    authMode = if (authMode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN
                    // Clear errors and potentially fields when switching modes
                    emailError = null
                    passwordError = null
                    confirmPasswordError = null
                    // Optionally clear fields:
                    // email = ""
                    // password = ""
                    // confirmPassword = ""
                },
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Optional: Or Sign in with
        // Spacer(modifier = Modifier.height(32.dp))
        // Row(verticalAlignment = Alignment.CenterVertically) {
        //     Divider(modifier = Modifier.weight(1f))
        //     Text(" OR ", modifier = Modifier.padding(horizontal = 8.dp), color = Color.Gray)
        //     Divider(modifier = Modifier.weight(1f))
        // }
        // Spacer(modifier = Modifier.height(16.dp))
        // SocialLoginButtons() // You'd create this composable for Google, Facebook etc.
    }
}

// @Composable
// fun SocialLoginButtons() {
//     // Placeholder for social login buttons
//     Row(
//         modifier = Modifier.fillMaxWidth(),
//         horizontalArrangement = Arrangement.SpaceEvenly
//     ) {
//         OutlinedButton(onClick = { /* TODO: Google Sign In */ }) { Text("Google") }
//         OutlinedButton(onClick = { /* TODO: Facebook Sign In */ }) { Text("Facebook") }
//     }
// }


@Preview(showBackground = true, name = "Login Mode")
@Composable
fun LoginPageLoginPreview() {
    GoogleWorldWebTheme { // Replace with your actual theme if different
        Surface {
            LoginPage(
                onLoginClicked = { _, _ -> },
                onSignUpClicked = { _, _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "Sign Up Mode")
@Composable
fun LoginPageSignUpPreview() {
    GoogleWorldWebTheme { // Replace with your actual theme if different
        Surface {
            // Simulate being in Sign Up mode for the preview
            var authMode by remember { mutableStateOf(AuthMode.SIGNUP) } // This won't work directly in preview like this
            // For a better preview of signup, you might need a wrapper.
            // Or just manually test by running the app and toggling.
            // For simplicity, this preview will still initially show Login
            // but the components for signup are present.
            LoginPage(
                onLoginClicked = { _, _ -> },
                onSignUpClicked = { _, _, _ -> }
            )
        }
    }
}
