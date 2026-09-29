package com.example.analysis

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Fixtures {
    fun zip(root: String, vararg files: Pair<String, String>): ByteArray {
        val b = ByteArrayOutputStream()
        ZipOutputStream(b).use { z ->
            files.forEach { (n, c) -> z.putNextEntry(ZipEntry(if (root.isEmpty()) n else "$root/$n")); z.write(c.toByteArray()); z.closeEntry() }
        }
        return b.toByteArray()
    }

    fun read(root: String, vararg files: Pair<String, String>) = ZipCodebaseReader.read(ByteArrayInputStream(zip(root, *files)))

    val nextApp = arrayOf(
        "package.json" to """{"name":"shop","dependencies":{"next":"14.0.0","react":"18.2.0","stripe":"14.0.0","tailwindcss":"3.4.0"},"devDependencies":{"typescript":"5.0.0"}}""",
        "src/app/page.tsx" to "export default function Home() {\n  return <main><h1>Shop</h1></main>;\n}\n",
        "src/app/(auth)/signup/page.tsx" to """
'use client';
export default function SignupPage() {
  return (
    <form onSubmit={submit}>
      <input name="email" type="email" placeholder="Work email" required />
      <input name="password" type="password" placeholder="Password" required />
      <button type="submit">Create account</button>
    </form>
  );
}
""".trimStart(),
        "src/app/login/page.tsx" to """
export default function Login() {
  return (
    <form>
      <input name="email" type="email" />
      <input name="password" type="password" />
      <button type="submit">Sign in</button>
    </form>
  );
}
""".trimStart(),
        "src/app/orders/[id]/page.tsx" to "export default function Order() {\n  return <button id=\"download-invoice\">Download invoice</button>;\n}\n",
        "src/app/api/orders/route.ts" to "export async function GET() {\n  return Response.json([]);\n}\nexport async function POST(req: Request) {\n  return Response.json({});\n}\n",
        "node_modules/x/index.js" to "module.exports = 1",
        "public/logo.png" to "binary"
    )
}
