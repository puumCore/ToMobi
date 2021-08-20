function download(path, filename, div) {

    const anchor = document.createElement('a');
    anchor.href = path;
    anchor.download = filename;

    document.body.appendChild(anchor);

    anchor.click();
    anchor.onended = function() {
        document.body.removeChild(div);
    }

    document.body.removeChild(anchor);
}