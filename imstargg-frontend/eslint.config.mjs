import { FlatCompat } from "@eslint/eslintrc";
import { dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig, globalIgnores } from "eslint/config";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const compat = new FlatCompat({ baseDirectory: __dirname });

const eslintConfig = defineConfig([
  ...compat.extends("next/core-web-vitals", "next/typescript"),
  {
    files: ["**/*.ts", "**/*.tsx"],
    rules: {
      // queryKey 는 queryKeys.ts 로만 생성 — useQuery/useInfiniteQuery 에 인라인 배열 금지.
      "no-restricted-syntax": [
        "error",
        {
          selector:
            "CallExpression[callee.name=/^use(Query|InfiniteQuery)$/] Property[key.name='queryKey'] > ArrayExpression",
          message:
            "queryKey 는 queryKeys.* 로만 생성하세요 (src/hooks/queries/queryKeys.ts). 인라인 배열 리터럴 금지.",
        },
      ],
    },
  },
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;
