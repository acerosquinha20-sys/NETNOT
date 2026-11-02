# Nexus

Projeto Android para gerar APK debug pelo GitHub Actions.

## Recursos
- Configuração de IP e porta do servidor/proxy do Nexus.
- Modo de depuração Wi‑Fi.
- Overlay flutuante móvel.
- Contador de players.
- Distâncias demonstrativas.
- Modo de teste com 42 players.
- Pipeline GitHub Actions para `app-debug.apk`.

## Dados do jogo
O projeto não intercepta tráfego protegido nem lê a memória de jogos de terceiros.
Os dados reais devem vir de uma API/SDK ou integração autorizada. O overlay é apenas
a camada de apresentação.

## Build
Faça push para o GitHub. Em **Actions**, o workflow `Build APK Debug` compila o APK.
O arquivo aparece nos **Artifacts** como `nexus-debug-apk`.
