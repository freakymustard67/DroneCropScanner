# Sarvam AI Platform — Full API Reference

> **Sarvam AI** is India's full-stack sovereign AI platform. Provides Models & APIs for building Indian-language AI applications — speech processing, text generation, translation, and document intelligence.
>
> **Website:** https://www.sarvam.ai/
> **Dashboard:** https://dashboard.sarvam.ai/
> **Documentation:** https://docs.sarvam.ai/
> **Base URL:** `https://api.sarvam.ai`
> **SDK:** `pip install sarvamai` (Python) / `npm install sarvamai@latest` (JS)

---

## 1. Platform Overview

| Feature | Details |
|---|---|
| **Provider** | Sarvam AI (Axonwise Private Limited), Bengaluru, India |
| **Auth** | `api-subscription-key` header (HTTP 403 on failure, not 401) |
| **Sovereignty** | Built on sovereign Indian compute, fully India-operated |
| **Languages** | 22 Indian languages + English (varies per API) |
| **SDKs** | Python (`sarvamai`), JavaScript (`sarvamai`) |
| **API Specs** | [OpenAPI JSON](https://docs.sarvam.ai/openapi.json), [AsyncAPI JSON](https://docs.sarvam.ai/asyncapi.json) |
| **MCP Server** | `https://docs.sarvam.ai/_mcp/server` (for Claude Code, Cursor, etc.) |
| **Full Docs Corpus** | https://docs.sarvam.ai/llms-full.txt |
| **Cookbook (GitHub)** | https://github.com/sarvamai/sarvam-ai-cookbook |

---

## 2. Quickstart

### 2.1 Get an API Key

1. Sign up at https://dashboard.sarvam.ai/ (100 free credits, no credit card)
2. Create an API subscription key from the dashboard
3. Export it as an environment variable:

```bash
export SARVAM_API_KEY="your_api_key_here"
```

### 2.2 Install SDK

```bash
pip install -U sarvamai
```

### 2.3 Make Your First API Call

**Python:**
```python
from sarvamai import SarvamAI

client = SarvamAI(api_subscription_key="YOUR_API_KEY")

response = client.text.translate(
    input="Hi, My Name is Vinayak.",
    source_language_code="auto",
    target_language_code="gu-IN",
    speaker_gender="Male"
)
print(response)
```

**cURL:**
```bash
curl -X POST https://api.sarvam.ai/translate \
  -H "api-subscription-key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "input": "Hello, how are you?",
    "source_language_code": "auto",
    "target_language_code": "hi-IN"
  }'
```

---

## 3. Models Reference

### 3.1 Speech Models

| Model | Type | Version | Description |
|---|---|---|---|
| **Saaras** | Speech-to-Text | v3 | Domain-aware speech recognition. Modes: `transcribe`, `translate`, `verbatim`, `translit`, `codemix`. Supports 12 Indic languages. |
| **Bulbul** | Text-to-Speech | v3 | Natural voices across 11 Indic languages. 30+ speaker voices. Supports pitch, pace, volume, SSML. |
| **Saarika** (Legacy) | Speech-to-Text | v2.5 | Legacy STT model with multi-speaker handling and telephony optimization. |

### 3.2 Text Models

| Model | Type | Parameters | Description |
|---|---|---|---|
| **Sarvam-30B** | Chat/LLM | 30B | Multilingual LLM optimized for Indian languages. Strong reasoning, coding, conversation. |
| **Sarvam-105B** | Chat/LLM | 105B | Flagship model. SOTA Indian language understanding, reasoning, generation. |
| **Sarvam-M** (Deprecated) | Chat/LLM | 24B | Hybrid-reasoning model with Wikipedia grounding support. |

### 3.3 Translation Models

| Model | Type | Description |
|---|---|---|
| **Mayura** | Translation | Advanced multilingual translation. Customizable styles, script control, code-mixed content handling. |
| **Sarvam Translate** | Translation | Supports all 22 official Indian languages. Formal translation style, structured text optimization. |

### 3.4 Vision Model

| Model | Type | Parameters | Description |
|---|---|---|---|
| **Sarvam Vision** | VLM | 3B | State-space Vision Language Model. Document intelligence — text extraction, table conversion, structure preservation. 23 languages (22 Indian + English). |

**Sarvam Vision Limits:**
| Limit | Value |
|---|---|
| Max pages per PDF | 10 |
| Max images per ZIP | 10 |
| Max file size | 200 MB |
| Input formats | PDF, PNG, JPG, ZIP |
| Rate limit | 10 requests/minute (all plans) |

---

## 4. API Endpoints

### 4.1 Chat Completion

| Method | Endpoint | Description |
|---|---|---|
| POST | `/chat/completions` | Generate conversational responses. Supports 10 Indic languages + English. |

**Key Parameters:** `model` (sarvam-30b / sarvam-105b), `temperature`, `top_p`, `reasoning_effort`, `wiki_grounding`, `presence_penalty`, `frequency_penalty`, `seed`, `max_tokens`, `stop`

### 4.2 Speech-to-Text

| Method | Endpoint | Description |
|---|---|---|
| POST | `/speech-to-text` | REST — short audio (<2 min), synchronous |
| WebSocket | `/speech-to-text/stream` | Streaming — real-time transcription |
| POST | `/speech-to-text/job/initiate` | Batch — long audio (up to 2 hrs), async |
| POST | `/speech-to-text/job/upload` | Upload files for batch job |
| POST | `/speech-to-text/job/start` | Start batch processing |
| GET | `/speech-to-text/job/status` | Check batch job status |
| GET | `/speech-to-text/job/download` | Download batch results |

**Saaras v3 Modes:** `transcribe` (original language), `translate` (to English), `verbatim` (word-for-word), `translit` (romanization), `codemix` (mixed script)

### 4.3 Text-to-Speech

| Method | Endpoint | Description |
|---|---|---|
| POST | `/text-to-speech` | REST — synchronous conversion |
| POST | `/text-to-speech/stream` | HTTP streaming audio |
| WebSocket | `/text-to-speech/stream` | Real-time streaming TTS |

**Key Parameters:** `model` (bulbul:v3), `speaker`, `pace`, `sample_rate`, `output_audio_codec`, `output_audio_bitrate`, `max_chunk_length`, `min_buffer_size`

### 4.4 Text Processing

| Method | Endpoint | Description |
|---|---|---|
| POST | `/translate` | Translate text between languages |
| POST | `/transliterate` | Convert text between scripts (same language) |
| POST | `/detect-language` | Identify language of input text |

### 4.5 Document Digitization (Sarvam Vision)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/document-digitization/initialize` | Create a digitization job |
| POST | `/document-digitization/get-upload-urls` | Get presigned upload URLs |
| POST | `/document-digitization/start` | Start processing |
| GET | `/document-digitization/get-status` | Check job status |
| GET | `/document-digitization/get-download-urls` | Download structured results |

### 4.6 Pronunciation Dictionary

| Method | Endpoint | Description |
|---|---|---|
| POST | `/pronunciation-dictionary` | Create custom pronunciations |
| GET | `/pronunciation-dictionary` | List dictionaries |
| GET | `/pronunciation-dictionary/{id}` | Get dictionary details |
| PATCH | `/pronunciation-dictionary/{id}` | Update dictionary |
| DELETE | `/pronunciation-dictionary/{id}` | Delete dictionary |

---

## 5. Code Examples (Crop-Relevant APIs)

### 5.1 Sarvam Vision — Analyze Crop Image

**Python:**
```python
from sarvamai import SarvamAI

client = SarvamAI(api_subscription_key="YOUR_API_KEY")

# Initialize digitization job for a crop field image
response = client.vision.initialize(
    file=open("crop_field_image.jpg", "rb"),
    output_format="json"
)
print(response)
```

**cURL:**
```bash
curl -X POST https://api.sarvam.ai/document-digitization/initialize \
  -H "api-subscription-key: YOUR_API_KEY" \
  -F "file=@crop_field_image.jpg" \
  -F "output_format=json"
```

### 5.2 Chat Completion — Crop Disease Diagnosis

**Python:**
```python
from sarvamai import SarvamAI

client = SarvamAI(api_subscription_key="YOUR_API_KEY")

response = client.chat.completions.create(
    model="sarvam-105b",
    messages=[
        {
            "role": "system",
            "content": "You are an agricultural expert specializing in crop disease diagnosis. Provide structured analysis with: disease name, confidence level, severity (1-5), recommended treatment."
        },
        {
            "role": "user",
            "content": "The rice leaves in my field have brown spots with yellow halos. What could this be, and how should I treat it?"
        }
    ],
    temperature=0.3,
    wiki_grounding=True
)
print(response.choices[0].message.content)
```

**cURL:**
```bash
curl -X POST https://api.sarvam.ai/chat/completions \
  -H "api-subscription-key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "sarvam-105b",
    "messages": [
      {"role": "system", "content": "You are an agricultural expert."},
      {"role": "user", "content": "Rice leaves have brown spots with yellow halos. Diagnosis?"}
    ],
    "temperature": 0.3,
    "wiki_grounding": true
  }'
```

### 5.3 Translation — Localize Reports to Tamil

```python
from sarvamai import SarvamAI

client = SarvamAI(api_subscription_key="YOUR_API_KEY")

response = client.text.translate(
    input="Your field shows moderate crop health with NDVI index of 0.52. Consider irrigation.",
    source_language_code="en-IN",
    target_language_code="ta-IN"
)
print(response)
```

### 5.4 Speech-to-Text — Transcribe Farmer Voice Notes

```python
from sarvamai import SarvamAI

client = SarvamAI(api_subscription_key="YOUR_API_KEY")

response = client.speech_to_text.transcribe(
    file=open("farmer_voice_note.wav", "rb"),
    model="saaras:v3",
    mode="transcribe"
)
print(response)
```

---

## 6. Pricing & Rate Limits

### Pricing Model

- **Free Tier:** 100 free credits on signup (no credit card)
- **Billing:** Pay-as-you-go in Indian Rupees (INR)
- See full pricing: https://docs.sarvam.ai/api-reference-docs/pricing

### Rate Limits

| API | Limit |
|---|---|
| Document Digitization (Vision) | 10 requests/minute |
| Chat Completion | Varies by plan |
| Speech-to-Text | Varies by plan |
| Text-to-Speech | Varies by plan |

Rate limit headers returned: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

On HTTP 429 (rate limited), retry after the `Retry-After` header duration.

---

## 7. Relevance to Crop Monitoring Project

The Drone Crop Scanner project currently uses Gemini Flash / Groq as cloud AI providers for Pass 2 detailed crop analysis (see `team_report.md` §7). Sarvam AI could complement or replace these:

| Use Case | Sarvam AI API | Benefit |
|---|---|---|
| **Crop image analysis** | Sarvam Vision (3B VLM) | Extract text, detect anomalies from drone-captured field images. Supports 22 Indian languages. |
| **Crop disease chat** | Sarvam-105B Chat Completion | With `wiki_grounding=true`, provides fact-based disease diagnosis and treatment advice. 30B/105B models are competitive with GPT-class LLMs. |
| **Farmer localization** | Mayura / Sarvam Translate | Translate diagnosis reports and treatment instructions into Tamil, Hindi, Telugu, etc. |
| **Voice interface** | Bulbul TTS + Saaras STT | Farmers can speak in their language (voice notes) → transcribed → analyzed → results read back in their language. |
| **Field report digitization** | Sarvam Vision | Digitize handwritten field notes, printed fertilizer labels, or pesticide instructions from photos. |
| **Sovereign deployment** | On-prem / private cloud | Sarvam offers private cloud and on-premise deployment options — relevant if data sovereignty is a concern for agricultural data. |

### Integration Notes

- **No vision + chat combined endpoint yet**: Unlike Gemini (which accepts images directly in chat), Sarvam has separate Vision (document AI) and Chat Completion APIs. A two-step pipeline would be needed: (1) analyze image with Vision API → (2) feed extracted info + diagnosis query into Chat Completion API.
- **Wiki Grounding**: Sarvam-30B/105B support `wiki_grounding` — a RAG-based approach that retrieves relevant Wikipedia chunks for factual accuracy. This is useful for crop disease queries where accuracy matters.
- **Language Coverage**: All APIs support major Indian languages, making this a strong choice for the app's localization requirements (Tamil, Hindi, English as noted in §9 of team_report.md).

---

## 8. Error Handling

| HTTP Status | Meaning | Handling |
|---|---|---|
| 200 | Success | Parse response |
| 400 | Bad Request | Check required parameters |
| 403 | Auth failure | Check API key (note: 403, not 401) |
| 422 | Validation error | Check input limits (e.g., >10 pages for Vision) |
| 429 | Rate limited | Implement exponential backoff with `Retry-After` |
| 503 | Service unavailable | Retry with backoff |

---

## 9. Links & References

| Resource | URL |
|---|---|
| Dashboard | https://dashboard.sarvam.ai/ |
| API Documentation | https://docs.sarvam.ai/ |
| Full Docs Corpus (LLMs) | https://docs.sarvam.ai/llms-full.txt |
| MCP Server | https://docs.sarvam.ai/_mcp/server |
| OpenAPI Spec | https://docs.sarvam.ai/openapi.json |
| AsyncAPI Spec | https://docs.sarvam.ai/asyncapi.json |
| GitHub Cookbook | https://github.com/sarvamai/sarvam-ai-cookbook |
| SDK (PyPI) | https://pypi.org/project/sarvamai/ |
| SDK (npm) | https://www.npmjs.com/package/sarvamai |
| Discord Community | https://discord.com/invite/5rAsykttcs |
| Pricing | https://docs.sarvam.ai/api-reference-docs/pricing |
| Blog | https://www.sarvam.ai/blogs |
| Status / Trust Center | https://www.sarvam.ai/trust-center |
