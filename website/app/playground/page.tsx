import type { Metadata } from "next";
import { Playground } from "@/components/Playground";

export const metadata: Metadata = {
  title: "Playground — NativeBridge",
  description: "Interactive test harness for every NativeBridge API inside a WebView.",
};

export default function PlaygroundPage() {
  return <Playground />;
}
