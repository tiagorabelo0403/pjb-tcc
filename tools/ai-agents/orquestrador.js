const { consultarGPT } = require("./gpt-agent");

async function testar() {
  console.log("\n🤖 Consultando GPT-4o...\n");
  const resposta = await consultarGPT(
    "Como implementar Virtual Threads no Java 21 para melhorar performance?"
  );
  console.log("📗 GPT-4o responde:");
  console.log(resposta);
}

testar().catch(console.error);