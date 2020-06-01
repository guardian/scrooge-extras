import fs from 'fs';
import { TBufferedTransport, TCompactProtocol, TTransport } from 'thrift';
import { School, SchoolSerde } from '@guardian/school/school';

async function readFile(file: string): Promise<Buffer> {
    return new Promise((resolve, reject) => {
        fs.readFile(file, (error, data) => {
            if (error) {
                reject(error);
            } else {
                resolve(data)
            }
        });
    });
}


async function writeFile(buffer: Buffer, file: string): Promise<void> {
    return new Promise((resolve, reject) => {
        fs.writeFile(file, buffer, {}, () => {
            resolve();
        });
    });
}

async function toTransport(buffer: Buffer): Promise<TTransport> {
    return new Promise((resolve, reject) => {
        const writer = TBufferedTransport.receiver((transport, seqID) => {
            resolve(transport)
        }, 0);
        writer(buffer);
    });
}

async function decode(buffer: Buffer): Promise<School> {
    const inputBufferTransport = await toTransport(buffer);
    const inputProtocol = new TCompactProtocol(inputBufferTransport);
    return SchoolSerde.read(inputProtocol)
}

async function encode(school: School): Promise<Buffer> {
    return new Promise((resolve, reject) => {
        const outputBufferTransport = new TBufferedTransport(undefined, (buffer, seqId) => {
            resolve(buffer);
        });
        const outputProtocol = new TCompactProtocol(outputBufferTransport);
        SchoolSerde.write(outputProtocol, school);
        outputProtocol.flush();
    });
}

async function decodeEncode(input: string, output: string): Promise<void> {
    const inputBuffer = await readFile(input);
    const school = await decode(inputBuffer);

    console.log(JSON.stringify(school))

    const outputBuffer = await encode(school);
    await writeFile(outputBuffer, output)
}

const input = process.argv[2];
const output = process.argv[3];

decodeEncode(input, output)
    .then(_ => console.log("Done"))
    .catch(e => console.error(e));