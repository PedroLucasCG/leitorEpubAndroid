# 📚 Leitor de EPUB com Destaques e Anotações

Este projeto é um aplicativo Android para leitura de livros em formato EPUB, com suporte completo a **destaques interativos**, **anotações persistentes** e **navegação por capítulos**.

## ✨ Funcionalidades

* 📖 Leitura de arquivos EPUB com estilo personalizado
* ✍️ Seleção de texto com opção de adicionar anotações
* 🌟 Destaque visual de trechos anotados (tipo marca-texto)
* 🔍 Modal para visualização, edição ou exclusão da anotação
* 📌 Salvamento automático com Room (banco de dados local)
* 🔄 Navegação por capítulos e TOC (Sumário)
* 📂 Suporte a imagens embutidas no EPUB
* 🌟 Abertura direta em trecho anotado ao clicar em uma anotação

## 🛠️ Tecnologias Utilizadas

* **Kotlin**
* **Room (Jetpack)**
* **View Binding**
* **Coil / BitmapFactory** (renderização de imagens)
* **EpubLib** (parser EPUB)
* **Jsoup** (parser HTML)
* **Material3** + **AppBar com scroll automático**
* **Coroutines + LifecycleScope**

## 📦 Estrutura do Projeto

```
com.example.leitor/
├── ReaderActivity.kt             # Tela principal de leitura
├── data/
│   ├── AppDatabase.kt            # Banco de dados local
│   ├── book/                     # Entidade Book
│   └── annotation/               # Entidade Annotation
├── res/
│   ├── layout/
│   │   ├── reader_activity.xml
│   │   └── annotation_modal.xml
│   └── drawable/
│       └── toggle_button_selector.xml
└── AndroidManifest.xml
```

## 📲 Como Instalar e Rodar

1. Clone o repositório:

   ```bash
   git clone https://github.com/seu-usuario/leitor-epub.git
   ```

2. Abra o projeto no **Android Studio**.

3. Certifique-se de ativar as permissões de leitura de arquivos para acessar o EPUB via `content://`.

4. Compile o app:

   ```bash
   Build > Make Project (Ctrl+F9)
   ```

5. Execute em um dispositivo ou emulador com Android 10+.

## 📂 Formato de Anotação

As anotações são salvas com:

* `bookId`: referência ao livro
* `chapter`: índice do capítulo
* `paragraph`: índice do parágrafo
* `startSelection` / `endSelection`: offsets do trecho
* `content`: texto da anotação
* `createdAt`: data/hora do registro

## ⚠️ Observações

O app usa URIs persistentes para manter o acesso ao arquivo EPUB. Isso exige que o usuário selecione o livro via `Intent.ACTION_OPEN_DOCUMENT`.

O EPUB deve conter estrutura HTML válida com tags `<p>`, `<h1>`, `<img>`.
