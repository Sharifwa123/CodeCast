# CodeCast clone server (free)

Your phone cannot run voice-cloning or lip-sync models, so CodeCast calls a small server **you** run.
Free ways to host it: **Google Colab** (free GPU, session-limited) or a **Hugging Face Space** (free CPU: slow but works for short text).

## What it gives you
| CodeCast feature | Endpoint | Model | Licence (check yourself) |
|---|---|---|---|
| Narration in **your own voice** | `POST /tts` | [Chatterbox](https://github.com/resemble-ai/chatterbox) | MIT |
| **Moving, lip-synced presenter** from your photo | `POST /talking-head` | SadTalker (optional) | check the repo/weights |

XTTS-v2 and the F5-TTS weights are non-commercial only, so they are not used here.

## Colab (free GPU) in 4 cells
```python
!git clone https://github.com/<you>/CodeCast && cd CodeCast/tools/clone-server && pip -q install -r requirements.txt
!pip -q install pyngrok
import os; os.environ["CLONE_TOKEN"] = "choose-a-secret"
from pyngrok import ngrok; ngrok.set_auth_token("<free ngrok token>")
import subprocess; subprocess.Popen(["uvicorn", "app:app", "--port", "8000"], cwd="CodeCast/tools/clone-server")
print(ngrok.connect(8000).public_url)   # paste this URL into CodeCast
```
Optional talking head: `git clone https://github.com/OpenTalker/SadTalker`, follow its install, then `os.environ["SADTALKER_DIR"]="/content/SadTalker"` before starting uvicorn.

## In CodeCast
Duration & Presentation step -> "Clone voice and presenter": paste the URL and token, record 10 to 15 seconds of your voice (or pick an audio file), tap **Test connection**.
The first request loads the model (minutes). If the server is unreachable CodeCast falls back to the phone's own voice and says so in the quality report.

Only clone a voice or face you own or have consent to use.
