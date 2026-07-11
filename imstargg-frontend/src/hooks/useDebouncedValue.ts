import { useEffect, useState } from "react";

/**
 * 값이 `ms` 동안 변하지 않고 안정된 뒤에야 갱신되는 디바운스된 값을 반환한다.
 * 라이브 검색처럼 입력마다 즉시 요청하지 않고, 타이핑이 멈춘 뒤에만
 * 다운스트림(useQuery 등)을 트리거하고 싶을 때 사용한다.
 */
export function useDebouncedValue<T>(value: T, ms: number): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const handle = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(handle);
  }, [value, ms]);

  return debounced;
}
