"""
CodeCast clone server: free, self-hosted voice cloning (+ optional talking-head video).

Endpoints (what the CodeCast app calls):
  GET  /health
  POST /tts           form: text, language, reference (audio file)  -> audio/wav   (Chatterbox, MIT licence)
  POST /talking-head  form: image (file), audio (file)              -> video/mp4   (optional, needs SadTalker; see README)

Set CLONE_TOKEN to require "Authorization: Bearer <token>".
NOTE: written to match the app's client, which is unit-tested against a mock server. This file itself has
not been run against the real models in CI. Check each model's licence before commercial use.
"""
import io
import os
import subprocess
import tempfile
import glob

import torchaudio
from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile
from fastapi.responses import FileResponse, Response

app = FastAPI()
TOKEN = os.environ.get("CLONE_TOKEN", "")
_tts = None


def auth(header):
    if TOKEN and header != f"Bearer {TOKEN}":
        raise HTTPException(status_code=401, detail="bad token")


def model():
    global _tts
    if _tts is None:
        import torch
        from chatterbox.tts import ChatterboxTTS
        _tts = ChatterboxTTS.from_pretrained(device="cuda" if torch.cuda.is_available() else "cpu")
    return _tts


@app.get("/health")
def health():
    return {"ok": True}


@app.post("/tts")
async def tts(text: str = Form(...), language: str = Form("en"), reference: UploadFile = File(...),
              authorization: str = Header(None)):
    auth(authorization)
    with tempfile.TemporaryDirectory() as d:
        ref = os.path.join(d, "ref" + os.path.splitext(reference.filename or "ref.wav")[1])
        with open(ref, "wb") as f:
            f.write(await reference.read())
        m = model()
        wav = m.generate(text, audio_prompt_path=ref)  # tensor [1, n]
        buf = io.BytesIO()
        torchaudio.save(buf, wav.cpu(), m.sr, format="wav", encoding="PCM_S", bits_per_sample=16)
        return Response(buf.getvalue(), media_type="audio/wav")


@app.post("/talking-head")
async def talking_head(image: UploadFile = File(...), audio: UploadFile = File(...), authorization: str = Header(None)):
    auth(authorization)
    sad = os.environ.get("SADTALKER_DIR")
    if not sad:
        raise HTTPException(status_code=501, detail="Set SADTALKER_DIR to a SadTalker checkout to enable this endpoint")
    with tempfile.TemporaryDirectory() as d:
        img = os.path.join(d, "face" + os.path.splitext(image.filename or "face.jpg")[1])
        aud = os.path.join(d, "audio.wav")
        open(img, "wb").write(await image.read())
        open(aud, "wb").write(await audio.read())
        out = os.path.join(d, "out")
        subprocess.run(["python", "inference.py", "--driven_audio", aud, "--source_image", img, "--result_dir", out,
                        "--still", "--preprocess", "full", "--enhancer", "gfpgan"], cwd=sad, check=True)
        files = sorted(glob.glob(os.path.join(out, "**", "*.mp4"), recursive=True))
        if not files:
            raise HTTPException(status_code=500, detail="SadTalker produced no video")
        data = open(files[-1], "rb").read()
        return Response(data, media_type="video/mp4")
