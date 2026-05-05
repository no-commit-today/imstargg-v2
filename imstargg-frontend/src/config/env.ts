import { z } from "zod";

const schema = z.object({
  NEXT_PUBLIC_API_BASE_URL: z.string().url(),
  NEXT_PUBLIC_SITE_URL: z.string().url().default("https://imstargg.com"),
  NEXT_PUBLIC_APP_VERSION: z.string(),
  NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT: z
    .string()
    .default("ca-pub-5084387143997125"),
});

export const env = schema.parse({
  NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL,
  NEXT_PUBLIC_SITE_URL: process.env.NEXT_PUBLIC_SITE_URL,
  NEXT_PUBLIC_APP_VERSION: process.env.NEXT_PUBLIC_APP_VERSION,
  NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT:
    process.env.NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT,
});

export type Env = z.infer<typeof schema>;
