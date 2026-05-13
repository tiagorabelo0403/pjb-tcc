const OpenAI = require("openai");

const client = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
});

async function consultarGPT(pergunta) {
  const response = await client.chat.completions.create({
    model: "gpt-4o",
    messages: [{ role: "user", content: pergunta }],
  });
  return response.choices[0].message.content;
}

module.exports = { consultarGPT };