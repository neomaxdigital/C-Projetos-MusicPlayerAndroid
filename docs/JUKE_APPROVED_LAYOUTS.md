# JUKE Mp3 Player — layouts aprovados

Dimensão dos seis layouts aprovados: **941 × 1672 px**.

Estas imagens são a **fonte visual oficial** do redesign. Elas não devem ser tratadas como inspiração para recriação livre.

## Recursos Android esperados

Colocar os PNGs em `app/src/main/res/drawable-nodpi/` com estes nomes:

- `juke_layout_library.png` — Biblioteca / Faixas
- `juke_layout_now_playing.png` — Tocando Agora
- `juke_layout_playlists.png` — Playlists
- `juke_layout_equalizer.png` — Equalizador
- `juke_layout_queue.png` — Fila de reprodução
- `juke_layout_settings.png` — Configurações

## Regra de implementação

1. Preservar o layout visual aprovado.
2. Usar os PNGs aprovados como base visual real da tela.
3. Sobrepor somente os elementos que precisam ser dinâmicos/interativos.
4. Reutilizar as funções já existentes no player atual; não recriar a lógica.
5. Não reintroduzir despertador.
6. Tema padrão: preto, grafite, cinza e branco; sem azul/ciano/glow/neon.
7. Trabalhar uma tela por vez.
8. Primeira tela: **Biblioteca / Faixas**.

## Mapeamento funcional — Biblioteca / Faixas

Manter do projeto atual:
- biblioteca manual via SAF;
- Faixas / Artistas / Álbuns / Playlists / Pastas;
- busca;
- abrir música;
- menu de faixa;
- favoritos;
- tocar a seguir;
- adicionar ao final da fila;
- adicionar à playlist;
- abrir artista/álbum;
- remover apenas da biblioteca;
- conversão WAV→MP3 quando aplicável;
- adicionar música;
- adicionar pasta;
- atualizar biblioteca;
- mini player funcional.

A imagem deve determinar posição, proporção, espaçamento e aparência. A lógica deve continuar vindo das funções existentes.
