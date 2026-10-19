# Casa para TCL

Launcher Android TV minimalista para a TCL 43S6500 com Android TV 9.

A interface substitui a tela inicial padrão por uma experiência leve, focada no controle remoto e nos recursos usados na TV:

- atalhos para continuar o último aplicativo ou entrada usada;
- entradas físicas, incluindo TV ao vivo, com nomes editáveis;
- dock horizontal de aplicativos, com organização e ocultação em **Ajustar Casa**;
- previsão do tempo baseada na localização da TV;
- papel de parede dinâmico em cache;
- tema escuro com foco visível, cartões translúcidos e navegação sem anúncios ou recomendações.

As configurações e a lista de aplicativos são mantidas localmente. O aplicativo usa a rede somente para atualizar o papel de parede e a previsão do tempo.

## Compilar

O projeto usa Android Gradle Plugin 8.5.2 e Java 17. Com o Android SDK instalado:

```sh
gradle assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Tornar a tela inicial padrão

Depois de instalar, abra o app e pressione o botão Home. O Android TV deve oferecer a escolha do app de início. Escolha **Casa** e marque a opção para sempre usar essa escolha, se aparecer.

Alguns firmwares TCL insistem em reabrir o launcher original. Só depois de conferir que o Casa funciona como esperado, é possível configurá-lo como Home por ADB. O procedimento de instalação definitiva ficará documentado quando testarmos a TV.
