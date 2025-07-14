import { registerPlugin } from '@capacitor/core';

import type { BarcodePlugin } from './definitions';

const Barcode = registerPlugin<BarcodePlugin>('Barcode', {
  web: () => import('./web').then((m) => new m.BarcodeWeb()),
});

export * from './definitions';
export { Barcode };
