package com.example.data.repository

import com.example.data.model.CodebaseFile

object DefaultCodebaseData {

    fun getDefaultCodebase(framework: String = "Next.js 14 + TypeScript"): List<CodebaseFile> {
        return listOf(
            CodebaseFile(
                path = "src/app/api/auth/register/route.ts",
                language = "typescript",
                lineCount = 42,
                sizeBytes = 1420,
                category = "Backend",
                exportedSymbols = listOf("POST", "RegisterSchema", "handleRegister"),
                content = """
import { NextResponse } from 'next/server';
import { z } from 'zod';
import bcrypt from 'bcryptjs';
import { prisma } from '@/lib/prisma';
import { signJwtToken } from '@/lib/auth';

const RegisterSchema = z.object({
  email: z.string().email({ message: "Invalid corporate email address" }),
  password: z.string().min(8, { message: "Password must be at least 8 characters" }),
  name: z.string().min(2, { message: "Full name is required" }),
  companyName: z.string().optional()
});

export async function POST(req: Request) {
  try {
    const body = await req.json();
    const validated = RegisterSchema.parse(body);

    const existingUser = await prisma.user.findUnique({
      where: { email: validated.email }
    });

    if (existingUser) {
      return NextResponse.json({ error: "User with this email already exists" }, { status: 409 });
    }

    const hashedPassword = await bcrypt.hash(validated.password, 12);

    const user = await prisma.user.create({
      data: {
        email: validated.email,
        name: validated.name,
        passwordHash: hashedPassword,
        role: "ADMIN"
      }
    });

    const token = await signJwtToken({ userId: user.id, email: user.email, role: user.role });

    return NextResponse.json({
      success: true,
      user: { id: user.id, email: user.email, name: user.name },
      token
    }, { status: 201 });
  } catch (err: any) {
    return NextResponse.json({ error: err.message || "Failed to register user" }, { status: 400 });
  }
}
""".trimIndent()
            ),

            CodebaseFile(
                path = "src/components/RegisterModal.tsx",
                language = "typescript",
                lineCount = 54,
                sizeBytes = 1890,
                category = "Frontend",
                exportedSymbols = listOf("RegisterModal", "handleSubmit", "useRegisterForm"),
                content = """
'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';

export function RegisterModal({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  async function handleRegister(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, name })
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Registration failed');

      localStorage.setItem('auth_token', data.token);
      router.push('/dashboard');
      onClose();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
      <form onSubmit={handleRegister} className="bg-slate-900 border border-slate-800 rounded-xl p-6 w-full max-w-md">
        <h2 className="text-xl font-bold text-white mb-2">Create your PayFlex Account</h2>
        <p className="text-sm text-slate-400 mb-4">Start processing payments and automated invoices in minutes.</p>

        {error && <div className="p-3 mb-3 bg-rose-500/10 border border-rose-500/30 rounded text-rose-400 text-xs">{error}</div>}

        <input type="text" placeholder="Full Name" value={name} onChange={e => setName(e.target.value)} required
          className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded mb-3 text-white focus:border-indigo-500 outline-none" />
        <input type="email" placeholder="Work Email" value={email} onChange={e => setEmail(e.target.value)} required
          className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded mb-3 text-white focus:border-indigo-500 outline-none" />
        <input type="password" placeholder="Password (min 8 chars)" value={password} onChange={e => setPassword(e.target.value)} required
          className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded mb-4 text-white focus:border-indigo-500 outline-none" />

        <button type="submit" disabled={loading}
          className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded transition">
          {loading ? 'Verifying Account...' : 'Continue to Dashboard'}
        </button>
      </form>
    </div>
  );
}
""".trimIndent()
            ),

            CodebaseFile(
                path = "src/lib/auth.ts",
                language = "typescript",
                lineCount = 38,
                sizeBytes = 1250,
                category = "Backend",
                exportedSymbols = listOf("signJwtToken", "verifySession", "hasRole"),
                content = """
import { SignJWT, jwtVerify } from 'jose';

const JWT_SECRET = new TextEncoder().encode(process.env.JWT_SECRET || 'codecast_jwt_secret_token_123');

export interface SessionPayload {
  userId: string;
  email: string;
  role: 'ADMIN' | 'MEMBER' | 'VIEWER';
}

export async function signJwtToken(payload: SessionPayload): Promise<string> {
  return new SignJWT({ ...payload })
    .setProtectedHeader({ alg: 'HS256' })
    .setIssuedAt()
    .setExpirationTime('7d')
    .sign(JWT_SECRET);
}

export async function verifySession(token: string): Promise<SessionPayload | null> {
  try {
    const { payload } = await jwtVerify(token, JWT_SECRET);
    return payload as unknown as SessionPayload;
  } catch {
    return null;
  }
}

export function hasRole(current: string, required: 'ADMIN' | 'MEMBER'): boolean {
  if (current === 'ADMIN') return true;
  return current === required;
}
""".trimIndent()
            ),

            CodebaseFile(
                path = "src/app/api/checkout/session/route.ts",
                language = "typescript",
                lineCount = 45,
                sizeBytes = 1620,
                category = "Backend",
                exportedSymbols = listOf("POST", "createCheckoutSession"),
                content = """
import { NextResponse } from 'next/server';
import Stripe from 'stripe';
import { prisma } from '@/lib/prisma';
import { verifySession } from '@/lib/auth';

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY || 'sk_test_mock_123', {
  apiVersion: '2023-10-16'
});

export async function POST(req: Request) {
  const token = req.headers.get('authorization')?.replace('Bearer ', '');
  const session = token ? await verifySession(token) : null;
  if (!session) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const { items, currency = 'USD' } = await req.json();

  const stripeSession = await stripe.checkout.sessions.create({
    payment_method_types: ['card'],
    line_items: items.map((item: any) => ({
      price_data: {
        currency,
        product_data: { name: item.title, description: item.sku },
        unit_amount: Math.round(item.unitPrice * 100)
      },
      quantity: item.quantity
    })),
    mode: 'payment',
    success_url: `${'$'}{process.env.NEXTAUTH_URL}/checkout/success?session_id={CHECKOUT_SESSION_ID}`,
    cancel_url: `${'$'}{process.env.NEXTAUTH_URL}/checkout/cancel`
  });

  return NextResponse.json({
    id: stripeSession.id,
    url: stripeSession.url
  });
}
""".trimIndent()
            ),

            CodebaseFile(
                path = "prisma/schema.prisma",
                language = "prisma",
                lineCount = 48,
                sizeBytes = 1450,
                category = "Database",
                exportedSymbols = listOf("User", "Order", "Product", "Invoice"),
                content = """
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

generator client {
  provider = "prisma-client-js"
}

enum Role {
  ADMIN
  MEMBER
  VIEWER
}

enum OrderStatus {
  PENDING
  PAID
  FULFILLED
  REFUNDED
}

model User {
  id           String    @id @default(cuid())
  email        String    @unique
  name         String?
  passwordHash String
  role         Role      @default(MEMBER)
  orders       Order[]
  createdAt    DateTime  @default(now())
  updatedAt    DateTime  @updatedAt
}

model Product {
  id          String   @id @default(cuid())
  name        String
  sku         String   @unique
  price       Decimal  @db.Decimal(10, 2)
  stock       Int      @default(0)
  createdAt   DateTime @default(now())
}

model Order {
  id          String      @id @default(cuid())
  userId      String
  user        User        @relation(fields: [userId], references: [id])
  totalAmount Decimal     @db.Decimal(10, 2)
  status      OrderStatus @default(PENDING)
  createdAt   DateTime    @default(now())
}
""".trimIndent()
            ),

            CodebaseFile(
                path = "src/components/DashboardAnalytics.tsx",
                language = "typescript",
                lineCount = 42,
                sizeBytes = 1530,
                category = "Frontend",
                exportedSymbols = listOf("DashboardAnalytics", "MetricCard"),
                content = """
'use client';

import React from 'react';

export function DashboardAnalytics({ mrr, activeSubscribers }: { mrr: number; activeSubscribers: number }) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
        <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Monthly Recurring Revenue</span>
        <div className="text-2xl font-bold text-emerald-400 mt-1">${'$'}{mrr.toLocaleString()}</div>
        <p className="text-xs text-emerald-500 mt-2">↑ 18.4% from last billing cycle</p>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
        <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Active Customers</span>
        <div className="text-2xl font-bold text-white mt-1">{activeSubscribers}</div>
        <p className="text-xs text-indigo-400 mt-2">Verified via Paystack & Stripe</p>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
        <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Payment Success Rate</span>
        <div className="text-2xl font-bold text-cyan-400 mt-1">99.82%</div>
        <p className="text-xs text-slate-400 mt-2">Webhook SLA: 42ms response</p>
      </div>
    </div>
  );
}
""".trimIndent()
            ),

            CodebaseFile(
                path = "src/lib/notifications/whatsapp.ts",
                language = "typescript",
                lineCount = 36,
                sizeBytes = 1180,
                category = "Backend",
                exportedSymbols = listOf("sendWhatsAppOrderNotification", "WhatsAppPayload"),
                content = """
import axios from 'axios';

const WHATSAPP_API_URL = 'https://graph.facebook.com/v19.0';

export async function sendWhatsAppOrderNotification(phone: string, orderId: string, total: string) {
  const token = process.env.WHATSAPP_CLOUD_API_TOKEN;
  const phoneId = process.env.WHATSAPP_PHONE_NUMBER_ID;

  if (!token || !phoneId) {
    console.warn("WhatsApp credentials not configured; skipping notification");
    return { skipped: true };
  }

  const payload = {
    messaging_product: 'whatsapp',
    to: phone,
    type: 'template',
    template: {
      name: 'order_receipt_confirmation',
      language: { code: 'en_US' },
      components: [
        {
          type: 'body',
          parameters: [
            { type: 'text', text: orderId },
            { type: 'text', text: total }
          ]
        }
      ]
    }
  };

  const response = await axios.post(`${'$'}{WHATSAPP_API_URL}/${'$'}{phoneId}/messages`, payload, {
    headers: { Authorization: `Bearer ${'$'}{token}` }
  });

  return response.data;
}
""".trimIndent()
            ),

            CodebaseFile(
                path = "package.json",
                language = "json",
                lineCount = 28,
                sizeBytes = 940,
                category = "Config",
                exportedSymbols = listOf("scripts", "dependencies"),
                content = """
{
  "name": "payflex-billing-core",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "prisma:push": "prisma db push",
    "test": "vitest run"
  },
  "dependencies": {
    "@prisma/client": "^5.10.2",
    "bcryptjs": "^2.4.3",
    "jose": "^5.2.2",
    "next": "14.1.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "stripe": "^14.18.0",
    "zod": "^3.22.4"
  },
  "devDependencies": {
    "prisma": "^5.10.2",
    "tailwindcss": "^3.4.1",
    "typescript": "^5.3.3"
  }
}
""".trimIndent()
            )
        )
    }

    fun parseZipStream(inputStream: java.io.InputStream): List<CodebaseFile> {
        val files = mutableListOf<CodebaseFile>()
        try {
            java.util.zip.ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null && files.size < 40) {
                    val name = entry.name
                    val lower = name.lowercase()
                    val isCodeFile = !entry.isDirectory &&
                            !lower.contains("node_modules/") &&
                            !lower.contains(".git/") &&
                            !lower.contains("/dist/") &&
                            !lower.contains("/build/") &&
                            (lower.endsWith(".ts") || lower.endsWith(".tsx") || lower.endsWith(".js") || lower.endsWith(".jsx") ||
                             lower.endsWith(".json") || lower.endsWith(".py") || lower.endsWith(".dart") || lower.endsWith(".kt") ||
                             lower.endsWith(".prisma") || lower.endsWith(".sql") || lower.endsWith(".vue") || lower.endsWith(".html") ||
                             lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".md"))

                    if (isCodeFile) {
                        val buffer = java.io.ByteArrayOutputStream()
                        val temp = ByteArray(4096)
                        var read: Int
                        var totalRead = 0
                        while (zis.read(temp).also { read = it } != -1 && totalRead < 35000) {
                            buffer.write(temp, 0, read)
                            totalRead += read
                        }
                        val contentStr = buffer.toString("UTF-8")
                        val lang = when {
                            lower.endsWith(".ts") || lower.endsWith(".tsx") -> "typescript"
                            lower.endsWith(".js") || lower.endsWith(".jsx") -> "javascript"
                            lower.endsWith(".py") -> "python"
                            lower.endsWith(".dart") -> "dart"
                            lower.endsWith(".kt") -> "kotlin"
                            lower.endsWith(".json") -> "json"
                            lower.endsWith(".prisma") -> "prisma"
                            lower.endsWith(".sql") -> "sql"
                            else -> "code"
                        }
                        val category = when {
                            lower.contains("route") || lower.contains("api") || lower.contains("server") || lower.contains("controller") -> "Backend"
                            lower.contains("schema") || lower.contains("model") || lower.contains("db") || lower.contains("migration") -> "Database"
                            lower.contains("config") || lower.contains(".json") || lower.contains(".yaml") -> "Config"
                            else -> "Frontend"
                        }
                        val lines = contentStr.lines()
                        val symbols = lines.filter { line ->
                            line.contains("export ") || line.contains("def ") || line.contains("class ") || line.contains("function ") || line.contains("model ")
                        }.take(4).map { it.trim().take(40) }

                        files.add(
                            CodebaseFile(
                                path = name,
                                language = lang,
                                lineCount = lines.size,
                                sizeBytes = entry.size.coerceAtLeast(contentStr.length.toLong()),
                                content = contentStr,
                                exportedSymbols = symbols,
                                category = category
                            )
                        )
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {
        }
        return if (files.isNotEmpty()) files else getDefaultCodebase()
    }
}
