package com.alorma.caliu.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.R
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuType

private val OuterCorner = 24.dp
private val InnerCorner = 8.dp
private val FieldMinHeight = 40.dp

/** On és una targeta dins d'un grup connectat: decideix quines cantonades són grans (24 dp) i quines petites (8 dp). */
enum class FormCardPosition { Single, First, Middle, Last }

/** Grup de targetes de formulari connectades, a 2 dp l'una de l'altra. */
@Composable
fun FormGroup(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(CaliuSpacing.joint),
    content = content,
  )
}

/** Una targeta del formulari: l'etiqueta en majúscules i el control a sota. */
@Composable
fun FormCard(
  label: String,
  position: FormCardPosition,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues =
    PaddingValues(
      start = CaliuSpacing.screen,
      end = CaliuSpacing.screen,
      top = CaliuSpacing.section,
      bottom = CaliuSpacing.screen,
    ),
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = position.shape(),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column {
      Text(
        text = label.uppercase(),
        style = CaliuType.sectionLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = CaliuSpacing.screen, end = CaliuSpacing.screen, top = CaliuSpacing.screen),
      )
      Column(modifier = Modifier.padding(contentPadding), content = content)
    }
  }
}

/** Camp de text dins d'una [FormCard]: `TextField` filled amb el contenidor transparent, perquè el fons és la targeta. */
@Composable
fun FormTextField(
  state: TextFieldState,
  label: String,
  position: FormCardPosition,
  modifier: Modifier = Modifier,
  imeAction: ImeAction = ImeAction.Next,
) {
  FormCard(label = label, position = position, modifier = modifier, contentPadding = PaddingValues(bottom = 4.dp)) {
    TextField(
      state = state,
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = FieldMinHeight)
          .semantics { contentDescription = label },
      textStyle = CaliuType.bodyLarge,
      lineLimits = TextFieldLineLimits.SingleLine,
      keyboardOptions = credentialKeyboard(imeAction),
      colors = transparentFieldColors(),
      contentPadding = FieldPadding,
    )
  }
}

/**
 * Camp secret (Access Secret): `SecureTextField` amagat, amb el botó de mostrar/amagar com a icona final.
 * [showLabel] i [hideLabel] són les descripcions del botó.
 */
@Composable
fun FormSecretField(
  state: TextFieldState,
  label: String,
  showLabel: String,
  hideLabel: String,
  position: FormCardPosition,
  modifier: Modifier = Modifier,
  initiallyVisible: Boolean = false,
  imeAction: ImeAction = ImeAction.Next,
) {
  var visible by rememberSaveable { mutableStateOf(initiallyVisible) }
  FormCard(label = label, position = position, modifier = modifier, contentPadding = PaddingValues(bottom = 4.dp)) {
    SecureTextField(
      state = state,
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = FieldMinHeight)
          .semantics { contentDescription = label },
      textStyle = CaliuType.bodyLarge,
      textObfuscationMode = if (visible) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
      keyboardOptions = credentialKeyboard(imeAction).copy(keyboardType = KeyboardType.Password),
      colors = transparentFieldColors(),
      contentPadding = FieldPadding,
      trailingIcon = {
        IconButton(onClick = { visible = !visible }) {
          Icon(
            painter =
              painterResource(if (visible) R.drawable.ic_visibility_off_filled else R.drawable.ic_visibility_filled),
            contentDescription = if (visible) hideLabel else showLabel,
          )
        }
      },
    )
  }
}

private val FieldPadding =
  PaddingValues(start = CaliuSpacing.screen, end = CaliuSpacing.screen, top = 4.dp, bottom = 8.dp)

// Les credencials de Tuya són identificadors: ni majúscules automàtiques ni correcció
private fun credentialKeyboard(imeAction: ImeAction) =
  KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = KeyboardType.Ascii,
    imeAction = imeAction,
  )

@Composable
private fun transparentFieldColors(): TextFieldColors =
  TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
  )

private fun FormCardPosition.shape(): RoundedCornerShape =
  when (this) {
    FormCardPosition.Single -> RoundedCornerShape(OuterCorner)
    FormCardPosition.First -> RoundedCornerShape(OuterCorner, OuterCorner, InnerCorner, InnerCorner)
    FormCardPosition.Middle -> RoundedCornerShape(InnerCorner)
    FormCardPosition.Last -> RoundedCornerShape(InnerCorner, InnerCorner, OuterCorner, OuterCorner)
  }

@CaliuPreviews
@Composable
private fun FormGroupPreview() {
  CaliuPreviewTheme {
    FormGroup {
      FormTextField(rememberTextFieldState("a1b2c3d4e5f6g7h8i9j0"), "Access ID", FormCardPosition.First)
      FormSecretField(
        state = rememberTextFieldState("secretsecretsecret"),
        label = "Access Secret",
        showLabel = "Mostra",
        hideLabel = "Amaga",
        position = FormCardPosition.Middle,
      )
      FormCard("Regió", FormCardPosition.Middle) {
        ChoiceSelector(listOf("EU", "US", "CN", "IN"), selectedIndex = 0, onSelect = {})
      }
      FormTextField(rememberTextFieldState("bf0123456789abcdefgh"), "Device ID", FormCardPosition.Last)
    }
  }
}
