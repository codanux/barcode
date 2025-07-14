export interface BarcodePlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
