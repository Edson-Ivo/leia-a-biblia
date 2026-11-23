# M 6:33

App Android nativo (Kotlin + Jetpack Compose) que pergunta “Você já leu a Bíblia hoje?” antes de abrir os apps que a própria pessoa escolheu. O nome vem de Mateus 6:33: “Mas buscai primeiro o reino de Deus, e a sua justiça, e todas estas coisas vos serão acrescentadas.”

Sem login, sem internet, sem anúncios. Tudo fica no aparelho.

## Como funciona

- A pessoa escolhe o app em que lê a Bíblia (por exemplo, YouVersion) e os apps que vão perguntar antes (com ícones e busca). Configurações, telefone, SMS, a tela inicial, o próprio M 6:33 e o app da Bíblia nunca podem ser marcados.
- Ao abrir um app escolhido num dia ainda não concluído, uma tela cobre o app: **Você já leu a Bíblia hoje?**
  - **Sim**: o app confere o tempo real que o app da Bíblia ficou em primeiro plano desde a meia-noite local (`UsageStatsManager`, eventos de atividade). Abaixo do mínimo (5 min por padrão, ajustável em 1, 3, 5, 10 ou 15), a resposta não é aceita e o app da Bíblia abre. Com o tempo atingido vem **Você fala a verdade?**; o segundo Sim conclui o dia.
  - **Não**: “Vou ler agora!” ou **Silenciar** (último valor customizado, 5, 10, 30 min ou horas e minutos). Nenhuma opção passa da meia-noite local.
- Tudo recomeça às 00:00 do fuso do aparelho.
- A árvore cresce com a sequência: Semente, Broto (dia 7), Muda, Arvoreta, Botões, Florada, Primeiros frutos, Frutífera, Frondosa (1 ano), Copa dourada, Lamparinas, Aurora e Árvore Iluminada (5 anos, 1825 dias). Um dia sem leitura volta à semente; a melhor sequência fica guardada. Marcos em uma semana, um mês, um semestre, um ano, cinco e dez anos.
- Um cordeiro flutua no Início. Ao toque, fala uma de 123 mensagens sobre ler a Bíblia, orar, ir à igreja e comunhão e correção, cada uma com os versículos que a sustentam (`app/src/main/assets/mensagens.json`).
- Tema claro ou escuro seguindo o sistema, com opção manual nos Ajustes.

## Permissões

| Permissão | Para quê | Como conceder |
| --- | --- | --- |
| Acesso de uso | Ver qual app veio para a frente e medir o tempo no app da Bíblia | Configurações › Apps › Acesso especial › Acesso de uso |
| Exibir sobre outros apps | Colocar a pergunta por cima do app aberto | Configurações › Apps › Acesso especial › Sobrepor a outros apps |
| Notificação (opcional) | Aviso fixo do serviço em primeiro plano | Diálogo do sistema |
| Bateria sem restrição (opcional) | Evitar que o fabricante encerre o app | Diálogo do sistema |

Não há serviço de acessibilidade. O Android não deixa um app comum travar outro: o M 6:33 cobre o app com uma janela, em geral um instante depois de ele abrir.

## Estrutura

- `core/`: regras do dia, sequência, Silenciar, fases da árvore e banco de mensagens. Sem dependência de Android, com testes em `app/src/test`.
- `guard/`: serviço em primeiro plano (`specialUse`) que consulta o app da frente só com a tela ligada e o dia pendente, e a janela por cima.
- `system/`: lista de apps com ícone (consulta declarada, sem `QUERY_ALL_PACKAGES`), uso real e permissões.
- `ui/`: telas em Compose.

## Compilar

```bash
./gradlew testDebugUnitTest assembleDebug
```

O APK sai em `app/build/outputs/apk/debug/app-debug.apk`. A versão de teste tem, nos Ajustes, uma seção “Simulação” para definir a sequência e ver as fases.
