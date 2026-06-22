# 🏃🏻‍➡️ Running

> Aplicativo Android de corrida, caminha e bike, para compartilhar com amigos.

---

## 📸 Screenshots

<table>
  <tr>
    <td align="center">
      <img src="app/src/main/res/drawable/fto1.jpeg" width="250"/><br/>
      <sub>Login</sub>
    </td>
    <td align="center">
      <img src="app/src/main/res/drawable/fto2.jpeg" width="250"/><br/>
      <sub>Register</sub>
    </td>
    <td align="center">
      <img src="app/src/main/res/drawable/fto3.jpeg" width="250"/><br/>
      <sub>Home</sub>
    </td>
  </tr>
</table>
<br>
<table>
  <tr>
    <td align="center">
      <img src="app/src/main/res/drawable/fto4.jpeg" width="250"/><br/>
      <sub>Activity</sub>
    </td>
    <td align="center">
      <img src="app/src/main/res/drawable/fto5.jpeg" width="250"/><br/>
      <sub>Tracking</sub>
    </td>
    <td align="center">
      <img src="app/src/main/res/drawable/fto6.jpeg" width="250"/><br/>
      <sub>Weather</sub>
    </td>
  </tr>
</table>

---

## 🎬 Vídeo Demonstrativo

<table>
  <tr>
    <td align="center">
      <a href="app/src/main/res/drawable/video.gif">
        <img src="app/src/main/res/drawable/fto1.jpeg" width="300" alt="Demonstração do App"/><br/>
      </a>
      <sub>Assista o vídeo demonstrativo</sub>
    </td>
  </tr>
</table>
---

## 📋 Funcionalidades

### 🔐 Autenticação
- **Login** com e-mail e senha via Firebase Authentication
- **Cadastro** de nova conta com confirmação de senha
- Suporte a **login biométrico** (impressão digital)
- Validação de campos em tempo real

### 🏠 Home
- Tela principal com **saudação personalizada** ao usuário
- **Card de clima** com temperatura e condição atual
- Acesso rápido às funcionalidades: Iniciar, Compartilhar e Chat

### 🏃 Atividades (Start)
- Seleção do tipo de atividade: **Correr**, **Caminhar** ou **Pedalar**
- Interface com cards interativos e ícones por tipo de atividade

### 📍 Rastreamento (Tracking)
- **Mapa em tempo real** com rota desenhada via OSMDroid
- **Cronômetro** de duração da atividade
- Exibição de **distância percorrida** (em metros/km)
- Cálculo de **calorias gastas** (kcal)
- Detecção automática de atividade via sensores do dispositivo
- Botões **Iniciar** e **Finalizar** com estado habilitado/desabilitado

### 🕒 Histórico
- **Lista de atividades** realizadas anteriormente
- **Tela de detalhe** com:
  - Tipo e data da atividade
  - Métricas: duração, distância e kcal
  - Mapa do percurso realizado
  - Foto registrada durante a atividade (quando disponível)

### 🌤️ Clima (Weather)
- **Veredicto visual** se o clima está favorável para atividades
- Cards de métricas: **luminosidade**, **temperatura** e **pressão atmosférica**
- Leitura via sensores do dispositivo

### 💬 Chat
- **Lista de conversas** com outros usuários
- **Sala de chat** em tempo real via Firebase Firestore
- Campo de mensagem com suporte a múltiplas linhas
- Botão de envio com feedback visual

---

## 🛠️ Tecnologias e Ferramentas

### Linguagem & Plataforma
| Tecnologia | Versão | Uso |
|---|---|---|
| **Kotlin** | 1.9+ | Linguagem principal |
| **Android SDK** | API 26+ | Plataforma alvo |
| **Gradle** | 8.x | Build system |

### UI / Design
| Ferramenta | Uso |
|---|---|
| **Material Design 3** | Componentes visuais (Cards, Buttons, TextInputLayout, Toolbar) |
| **CoordinatorLayout** | Layout base com AppBar scrollável |
| **ConstraintLayout** | Posicionamento preciso (splash screen) |
| **NestedScrollView** | Scroll em telas de detalhe |
| **RecyclerView** | Listas de chat e histórico |
| **Vector Drawables** | Ícones personalizados (24x24dp) |
| **Shape Drawables** | Gradientes, bordas arredondadas, fundos |

### Firebase
| Serviço | Uso |
|---|---|
| **Firebase Authentication** | Login, cadastro e autenticação biométrica |
| **Firebase Firestore** | Armazenamento de mensagens e histórico de atividades |
| **Firebase Storage** | Upload de fotos das atividades |

### Mapas
| Biblioteca | Uso |
|---|---|
| **OSMDroid** | Exibição de mapa offline e desenho de rota GPS |

### Sensores do Dispositivo
| Sensor | Uso |
|---|---|
| **GPS / Fused Location** | Rastreamento de rota em tempo real |
| **Activity Recognition API** | Detecção automática de tipo de atividade |
| **Light Sensor** | Medição de luminosidade (tela de Clima) |
| **Pressure Sensor** | Medição de pressão atmosférica |
| **Temperature Sensor** | Leitura de temperatura ambiente |

---

## 🚀 Como Executar

### Pré-requisitos
- Android Studio Hedgehog ou superior
- JDK 17
- Dispositivo ou emulador com API 26+

### Configuração
```bash
# Clone o repositório
git clone https://github.com/Tiago7mendes/canarinhapp.git
cd canarinhapp
```

1. Adicione o arquivo `google-services.json` na pasta `app/` (obtido no Firebase Console)
2. Sincronize o projeto com o Gradle
3. Execute no dispositivo ou emulador

### Dependências principais (build.gradle)
```groovy
dependencies {
    // Firebase
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-auth-ktx'
    implementation 'com.google.firebase:firebase-firestore-ktx'
    implementation 'com.google.firebase:firebase-storage-ktx'

    // Material Design 3
    implementation 'com.google.android.material:material:1.11.0'

    // OSMDroid (mapas)
    implementation 'org.osmdroid:osmdroid-android:6.1.17'

    // Activity Recognition
    implementation 'com.google.android.gms:play-services-location:21.1.0'
}
```

---

## 👨‍💻 Autores

**Cauan Mendes** **e**
**Tiago Setti Mendes**
- GitHub: [@Tiago7mendes](https://github.com/Tiago7mendes)
[@CauanMendes](https://github.com/cauanmendes)
- Curso: Análise e Desenvolvimento de Sistemas — IFSP Araraquara
